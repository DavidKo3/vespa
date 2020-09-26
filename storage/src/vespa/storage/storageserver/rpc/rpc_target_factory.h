// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include <vespa/vespalib/stllike/string.h>
#include <memory>

namespace storage::rpc {

struct RpcTarget;

/**
 * Factory for creating instances of RpcTarget based on a connection spec.
 */
class RpcTargetFactory {
public:
    virtual ~RpcTargetFactory() = default;
    virtual std::unique_ptr<RpcTarget> make_target(const vespalib::string& connection_spec, uint32_t slobrok_gen) const = 0;
};

}

