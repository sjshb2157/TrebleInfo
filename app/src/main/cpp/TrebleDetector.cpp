/*
 *     Treble Info
 *     Copyright (C) 2019-2022 Hackintosh Five
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: GPL-3.0-or-later

#include <climits>
#include <jni.h>

#include <vintf/parse_xml.h>
#include <vintf/CompatibilityMatrix.h>
#include <vintf/CheckFlags.h>
#include <vintf/ObjectFactory.h>
#include <vintf/VintfObject.h>
#include <utils/Errors.h>
#include "libvintf/utils.h"
#include <android-base/logging.h>

struct StubRuntimeInfo : public android::vintf::RuntimeInfo {
    android::status_t fetchAllInformation(FetchFlags) override { return android::UNKNOWN_ERROR; }
};

struct StaticRuntimeInfoFactory : public android::vintf::ObjectFactory<StubRuntimeInfo::RuntimeInfo> {
public:
    StaticRuntimeInfoFactory() = default;
    [[nodiscard]] std::shared_ptr<StubRuntimeInfo::RuntimeInfo> make_shared() const override {
        return std::make_shared<StubRuntimeInfo>();
    }
};

extern "C" JNIEXPORT jint JNICALL
Java_tk_hack5_treblecheck_data_TrebleDetector_check_1compatibility_1matrix(__unused JNIEnv *env, __unused jobject thiz, jstring matrixContentString) {
    using namespace android::vintf::details;
    const char* matrixContent = env->GetStringUTFChars(matrixContentString, nullptr);

    auto fileSystem = std::make_unique<FileSystemImpl>();
    std::string error;
/*
    android::status_t err = fileSystem->fetch(matrixPathChars, &xml, &error);
    if (err != android::OK) {
        LOG(ERROR) << "Cannot read '" << matrixPathChars << "' (" << strerror(-err) << "): " << error;
        return -1;
    }
*/
    auto matrix = std::make_unique<android::vintf::CompatibilityMatrix>();
    //matrix->setFileName(matrixPathChars);

    if (!fromXml(matrix.get(), matrixContent, &error)) {
        LOG(ERROR) << "Cannot parse packaged matrix: " << error;
        return -1;
    }

    auto propertyFetcher = std::make_unique<PropertyFetcherImpl>();

    auto vintfObject =
            android::vintf::VintfObject::Builder()
                    .setFileSystem(std::move(fileSystem))
                    .setPropertyFetcher(std::move(propertyFetcher))
                    .setRuntimeInfoFactory(std::make_unique<StaticRuntimeInfoFactory>())
                    .build();
    auto manifest = vintfObject->getDeviceHalManifest();
    bool ret = manifest->checkCompatibility(*matrix.get(), &error, android::vintf::CheckFlags::DISABLE_ALL_CHECKS);
    if (!error.empty()) {
        LOG(ERROR) << "Compatibility check failed: " << error;
    }

    env->ReleaseStringUTFChars(matrixContentString, matrixContent);
    return ret;
}
