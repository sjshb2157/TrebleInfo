// SPDX-License-Identifier: GPL-3.0-or-later

// An unprivileged app cannot read APEX VINTF data.

#include "Apex.h"

#include <utils/Errors.h>

namespace android::vintf::apex {

std::optional<timespec> GetModifiedTime(FileSystem*, PropertyFetcher*) {
    return std::nullopt;
}

status_t GetVendorVintfDirs(FileSystem*, PropertyFetcher*, std::vector<std::string>*,
                            std::string*) {
    return OK;
}

status_t GetOdmVintfDirs(FileSystem*, PropertyFetcher*, std::vector<std::string>*, std::string*) {
    return OK;
}

status_t GetFrameworkVintfDirs(FileSystem*, PropertyFetcher*, std::vector<std::string>*,
                               std::string*) {
    return OK;
}

}  // namespace android::vintf::apex
