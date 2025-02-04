// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef MOJO_PUBLIC_CPP_BASE_SHARED_MEMORY_VERSION_H_
#define MOJO_PUBLIC_CPP_BASE_SHARED_MEMORY_VERSION_H_

#include <atomic>
#include <cstdint>

#include "base/component_export.h"

#include "base/memory/read_only_shared_memory_region.h"

namespace mojo {

class SharedMemoryVersionClient;

using VersionType = uint64_t;
using SharedVersionType = std::atomic<VersionType>;
static_assert(SharedVersionType::is_always_lock_free,
              "Usage of SharedVersionType across processes might be unsafe");

// This file contains classes to share a version between processes through
// shared memory. A version is a nonzero monotonically increasing integer. A
// controller has read and write access to the version and one or many clients
// have read access only. Controllers should only be created in privileged
// processes. Clients can avoid issuing IPCs depending on the version stored in
// shared memory.
//
// Example:
//
//   class Controller : mojom::StateProvider {
//     ...
//
//     void SetState(State state) {
//       state_ = state;
//       version_.Increment();
//     }
//
//     void GetState(
//         base::OnceCallback<void(State, VersionType)> callback) override {
//       callback_.Run(state_, version_.GetSharedVersion());
//     }
//
//     SharedMemoryVersionController version_;
//   };
//
//   class Client {
//     ...
//
//     State GetState() {
//       // IPC can be avoided.
//       if (cached_version_ &&
//           !version_.SharedVersionIsGreaterThan(cached_version_)) {
//         return cached_state_.value();
//       }
//
//       State state;
//       VersionType version;
//
//       // Sync IPC to keep the example simple. Prefer async IPCs.
//       if (!provider_->GetState(&state, &version)) {
//         // error handling
//       }
//
//       cached_state_ = state;
//       cached_version_ = version;
//       return cached_state_.value();
//     }
//
//     mojo::Receiver<mojom::StateProvider> provider_;
//     std::optional<State> cached_state_;
//     std::optional<VersionType> cached_version_;
//     SharedMemoryVersionClient version_;
//
//   };

namespace shared_memory_version {

static constexpr VersionType kInvalidVersion = 0ULL;
static constexpr VersionType kInitialVersion = 1ULL;

}  // namespace shared_memory_version

// Used to modify the version number and issue read only handles to it.
class COMPONENT_EXPORT(MOJO_BASE) SharedMemoryVersionController {
 public:
  SharedMemoryVersionController();
  ~SharedMemoryVersionController() = default;

  // Not copyable or movable
  SharedMemoryVersionController(const SharedMemoryVersionController&) = delete;
  SharedMemoryVersionController& operator=(
      const SharedMemoryVersionController&) = delete;

  // Get a shared memory region to be sent to a different process. It will be
  // used to instantiate a SharedMemoryVersionClient.
  base::ReadOnlySharedMemoryRegion GetSharedMemoryRegion();

  VersionType GetSharedVersion();

  // Increment shared version. This is not expected to cause a wrap of the value
  // during normal operation. This invariant is guaranteed with a CHECK.
  void Increment();

  // Directly set shared version. `version` must be strictly larger than
  // previous version. `version` cannot be maximum representable value for
  // VersionType.
  void SetVersion(VersionType version);

 private:
  base::MappedReadOnlyRegion mapped_region_;
};

// Used to keep track of a remote version number and compare it to a
// locally tracked version.
class COMPONENT_EXPORT(MOJO_BASE) SharedMemoryVersionClient {
 public:
  explicit SharedMemoryVersionClient(
      base::ReadOnlySharedMemoryRegion shared_region);
  ~SharedMemoryVersionClient() = default;

  // Not copyable or movable
  SharedMemoryVersionClient(const SharedMemoryVersionClient&) = delete;
  SharedMemoryVersionClient& operator=(const SharedMemoryVersionClient&) =
      delete;

  // These functions can be used to form statements such as:
  // "Skip the IPC if `SharedVersionIsLessThan()` returns true. "
  // The functions err on the side of caution and return true if the comparison
  // is impossible since issuing an IPC should always be an option.
  bool SharedVersionIsLessThan(VersionType version);
  bool SharedVersionIsGreaterThan(VersionType version);

 private:
  VersionType GetSharedVersion();
  base::ReadOnlySharedMemoryRegion shared_region_;
  base::ReadOnlySharedMemoryMapping read_only_mapping_;
};

}  // namespace mojo

#endif  // MOJO_PUBLIC_CPP_BASE_SHARED_MEMORY_VERSION_H_
