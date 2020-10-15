// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "tensor_modify_update.h"
#include <vespa/document/base/exceptions.h>
#include <vespa/document/base/field.h>
#include <vespa/document/datatype/tensor_data_type.h>
#include <vespa/document/fieldvalue/document.h>
#include <vespa/document/fieldvalue/tensorfieldvalue.h>
#include <vespa/document/serialization/vespadocumentdeserializer.h>
#include <vespa/document/util/serializableexceptions.h>
#include <vespa/eval/eval/operation.h>
#include <vespa/eval/eval/value.h>
#include <vespa/eval/eval/engine_or_factory.h>
#include <vespa/eval/tensor/partial_update.h>
#include <vespa/eval/tensor/tensor.h>
#include <vespa/eval/tensor/cell_values.h>
#include <vespa/vespalib/objects/nbostream.h>
#include <vespa/vespalib/stllike/asciistream.h>
#include <vespa/vespalib/util/stringfmt.h>
#include <vespa/vespalib/util/xmlstream.h>
#include <ostream>

using vespalib::IllegalArgumentException;
using vespalib::IllegalStateException;
using vespalib::make_string;
using vespalib::eval::ValueType;
using vespalib::eval::EngineOrFactory;
using vespalib::tensor::TensorPartialUpdate;

using join_fun_t = double (*)(double, double);

namespace document {

namespace {

double
replace(double, double b)
{
    return b;
}
    
join_fun_t
getJoinFunction(TensorModifyUpdate::Operation operation)
{
    using Operation = TensorModifyUpdate::Operation;
    
    switch (operation) {
    case Operation::REPLACE:
        return replace;
    case Operation::ADD:
        return vespalib::eval::operation::Add::f;
    case Operation::MULTIPLY:
        return vespalib::eval::operation::Mul::f;
    default:
        throw IllegalArgumentException("Bad operation", VESPA_STRLOC);
    }
}

vespalib::string
getJoinFunctionName(TensorModifyUpdate::Operation operation)
{
    using Operation = TensorModifyUpdate::Operation;
    
    switch (operation) {
    case Operation::REPLACE:
        return "replace";
    case Operation::ADD:
        return "add";
    case Operation::MULTIPLY:
        return "multiply";
    default:
        throw IllegalArgumentException("Bad operation", VESPA_STRLOC);
    }
}

std::unique_ptr<const TensorDataType>
convertToCompatibleType(const TensorDataType &tensorType)
{
    std::vector<ValueType::Dimension> list;
    for (const auto &dim : tensorType.getTensorType().dimensions()) {
        list.emplace_back(dim.name);
    }
    return std::make_unique<const TensorDataType>(ValueType::tensor_type(std::move(list), tensorType.getTensorType().cell_type()));
}

}

IMPLEMENT_IDENTIFIABLE(TensorModifyUpdate, ValueUpdate);

TensorModifyUpdate::TensorModifyUpdate()
    : _operation(Operation::MAX_NUM_OPERATIONS),
      _tensorType(),
      _tensor()
{
}

TensorModifyUpdate::TensorModifyUpdate(const TensorModifyUpdate &rhs)
    : _operation(rhs._operation),
      _tensorType(rhs._tensorType->clone()),
      _tensor(Identifiable::cast<TensorFieldValue *>(_tensorType->createFieldValue().release()))
{
    *_tensor = *rhs._tensor;
}

TensorModifyUpdate::TensorModifyUpdate(Operation operation, std::unique_ptr<TensorFieldValue> tensor)
    : _operation(operation),
      _tensorType(Identifiable::cast<const TensorDataType &>(*tensor->getDataType()).clone()),
      _tensor(Identifiable::cast<TensorFieldValue *>(_tensorType->createFieldValue().release()))
{
    *_tensor = *tensor;
}

TensorModifyUpdate::~TensorModifyUpdate() = default;

TensorModifyUpdate &
TensorModifyUpdate::operator=(const TensorModifyUpdate &rhs)
{
    if (&rhs != this) {
        _operation = rhs._operation;
        _tensor.reset();
        _tensorType.reset(rhs._tensorType->clone());
        _tensor.reset(Identifiable::cast<TensorFieldValue *>(_tensorType->createFieldValue().release()));
        *_tensor = *rhs._tensor;
    }
    return *this;
}

TensorModifyUpdate &
TensorModifyUpdate::operator=(TensorModifyUpdate &&rhs)
{
    _operation = rhs._operation;
    _tensorType = std::move(rhs._tensorType);
    _tensor = std::move(rhs._tensor);
    return *this;
}

bool
TensorModifyUpdate::operator==(const ValueUpdate &other) const
{
    if (other.getClass().id() != TensorModifyUpdate::classId) {
        return false;
    }
    const TensorModifyUpdate& o(static_cast<const TensorModifyUpdate&>(other));
    if (_operation != o._operation) {
        return false;
    }
    if (*_tensor != *o._tensor) {
        return false;
    }
    return true;
}


void
TensorModifyUpdate::checkCompatibility(const Field& field) const
{
    if (field.getDataType().getClass().id() != TensorDataType::classId) {
        throw IllegalArgumentException(make_string("Cannot perform tensor modify update on non-tensor field '%s'",
                                                   field.getName().data()), VESPA_STRLOC);
    }
}


std::unique_ptr<vespalib::eval::Value>
old_modify(const vespalib::eval::Value *input,
           const vespalib::eval::Value *modify_spec,
           join_fun_t function)
{
    auto a = dynamic_cast<const vespalib::tensor::Tensor *>(input);
    // Cells tensor being sparse was validated during deserialize().
    auto b = dynamic_cast<const vespalib::tensor::SparseTensor *>(modify_spec);
    vespalib::tensor::CellValues cellValues(*b);
    return a->modify(function, cellValues);
}

std::unique_ptr<vespalib::eval::Value>
TensorModifyUpdate::applyTo(const vespalib::eval::Value &tensor) const
{
    auto cellsTensor = _tensor->getAsTensorPtr();
    if (cellsTensor) {
        auto engine = EngineOrFactory::get();
        if (engine.is_factory()) {
            return TensorPartialUpdate::modify(tensor, getJoinFunction(_operation), *cellsTensor, engine.factory());
        } else {
            return old_modify(&tensor, cellsTensor, getJoinFunction(_operation));
        }
    }
    return {};
}

bool
TensorModifyUpdate::applyTo(FieldValue& value) const
{
    if (value.inherits(TensorFieldValue::classId)) {
        TensorFieldValue &tensorFieldValue = static_cast<TensorFieldValue &>(value);
        auto oldTensor = tensorFieldValue.getAsTensorPtr();
        if (oldTensor) {
            auto newTensor = applyTo(*oldTensor);
            if (newTensor) {
                tensorFieldValue = std::move(newTensor);
            }
        }
    } else {
        vespalib::string err = make_string("Unable to perform a tensor modify update on a '%s' field value",
                                           value.getClass().name());
        throw IllegalStateException(err, VESPA_STRLOC);
    }
    return true;
}

void
TensorModifyUpdate::printXml(XmlOutputStream& xos) const
{
    xos << "{TensorModifyUpdate::printXml not yet implemented}";
}

void
TensorModifyUpdate::print(std::ostream& out, bool verbose, const std::string& indent) const
{
    out << indent << "TensorModifyUpdate(" << getJoinFunctionName(_operation) << ",";
    if (_tensor) {
        _tensor->print(out, verbose, indent);
    }
    out << ")";
}

namespace {

void
verifyCellsTensorIsSparse(const vespalib::eval::Value *cellsTensor)
{
    if (cellsTensor == nullptr) {
        return;
    }
    auto engine = EngineOrFactory::get();
    if (engine.is_factory()) {
        if (cellsTensor->type().is_sparse()) {
            return;
        }
    } else {
        if (dynamic_cast<const vespalib::tensor::SparseTensor *>(cellsTensor)) {
            return;
        }
    }
    vespalib::string err = make_string("Expected cells tensor to be sparse, but has type '%s'",
                                       cellsTensor->type().to_spec().c_str());
    throw IllegalStateException(err, VESPA_STRLOC);
}

}

void
TensorModifyUpdate::deserialize(const DocumentTypeRepo &repo, const DataType &type, nbostream & stream)
{
    uint8_t op;
    stream >> op;
    if (op >= static_cast<uint8_t>(Operation::MAX_NUM_OPERATIONS)) {
        vespalib::asciistream msg;
        msg << "Unrecognized tensor modify update operation " << static_cast<uint32_t>(op);
        throw DeserializeException(msg.str(), VESPA_STRLOC);
    }
    _operation = static_cast<Operation>(op);
    _tensorType = convertToCompatibleType(Identifiable::cast<const TensorDataType &>(type));
    auto tensor = _tensorType->createFieldValue();
    if (tensor->inherits(TensorFieldValue::classId)) {
        _tensor.reset(static_cast<TensorFieldValue *>(tensor.release()));
    } else {
        vespalib::string err = make_string("Expected tensor field value, got a '%s' field value",
                                           tensor->getClass().name());
        throw IllegalStateException(err, VESPA_STRLOC);
    }
    VespaDocumentDeserializer deserializer(repo, stream, Document::getNewestSerializationVersion());
    deserializer.read(*_tensor);
    verifyCellsTensorIsSparse(_tensor->getAsTensorPtr());
}

TensorModifyUpdate*
TensorModifyUpdate::clone() const
{
    return new TensorModifyUpdate(*this);
}

}
