// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <unordered_map>
#include <vespa/vespalib/data/slime/memory.h>
#include <vespa/vespalib/stllike/string.h>
#include "predicate_interval.h"
#include <climits>

namespace vespalib {
namespace slime { class Inspector; }
}  // namespace vespalib;

namespace search {
namespace predicate {

struct RangeFeature {
    vespalib::slime::Memory label;
    int64_t from;
    int64_t to;
};

constexpr uint32_t MIN_INTERVAL = 0x0001;
constexpr uint32_t MAX_INTERVAL = 0xffff;

struct PredicateTreeAnnotations {
    PredicateTreeAnnotations(uint32_t mf=0, uint16_t ir=MAX_INTERVAL)
            : min_feature(mf), interval_range(ir) {}
    uint32_t min_feature;
    uint16_t interval_range;
    std::unordered_map<uint64_t, std::vector<Interval>> interval_map;
    std::unordered_map<uint64_t, std::vector<IntervalWithBounds>> bounds_map;

    std::vector<uint64_t> features;
    std::vector<RangeFeature> range_features;
};

/**
 * Annotates a predicate document, represented by a slime object, with
 * intervals used for matching with the interval algorithm.
 */
struct PredicateTreeAnnotator {
    static void annotate(const vespalib::slime::Inspector &in,
                         PredicateTreeAnnotations &result,
                         int64_t lower_bound=LLONG_MIN,
                         int64_t upper_bound=LLONG_MAX);
};

}  // namespace predicate
}  // namespace search

