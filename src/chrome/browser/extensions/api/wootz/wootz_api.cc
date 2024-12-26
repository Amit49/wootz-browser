// Copyright 2012 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/browser/extensions/api/wootz/wootz_api.h"

#include <stddef.h>

#include <memory>
#include <optional>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

#include "base/android/build_info.h"
#include "base/android/jni_string.h"
#include "base/functional/bind.h"
#include "base/json/json_writer.h"
#include "base/lazy_instance.h"
#include "base/strings/utf_string_conversions.h"
#include "base/values.h"
#include "build/build_config.h"
#include "chrome/browser/extensions/extension_service.h"
// #include "chrome/android/chrome_jni_headers/WootzBridge_jni.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/wootz_wallet/wootz_wallet_service_factory.h"
#include "components/search_engines/template_url_service.h"
#include "components/wootz_wallet/browser/wootz_wallet_service.h"
#include "content/public/browser/web_contents.h"
#include "extensions/browser/event_router.h"
#include "extensions/browser/extension_event_histogram_value.h"
#include "extensions/browser/extension_function.h"
#include "extensions/browser/extension_prefs.h"
#include "extensions/browser/extension_prefs_factory.h"
#include "extensions/browser/extension_system.h"
#include "extensions/common/extension_id.h"
#include "ui/gfx/image/image.h"
#include "chrome/android/chrome_jni_headers/OpenExtensionsById_jni.h"
#include "base/android/shared_preferences/shared_preferences_manager.h"
#include "chrome/browser/preferences/android/chrome_shared_preferences.h"
#include "base/json/json_reader.h"
#include "base/json/values_util.h"
#include "base/time/time.h"

namespace extensions {

wootz_wallet::KeyringService* GetKeyringService(
    content::BrowserContext* context) {
  auto* profile = Profile::FromBrowserContext(context);
  return wootz_wallet::WootzWalletServiceFactory::GetServiceForContext(profile)
      ->keyring_service();
}

void OpenExtensionsById(const std::string& extensionId) {
  JNIEnv* env = base::android::AttachCurrentThread();
  Java_OpenExtensionsById_openExtensionByIdNative(
      env, 
      base::android::ConvertUTF8ToJavaString(env, extensionId));
}

void closeExtensionBottomSheet() {
  JNIEnv* env = base::android::AttachCurrentThread();
  Java_OpenExtensionsById_closeExtensionBottomSheetNative(
      env);
}

ExtensionId GetWootzWalletExtensionId(content::BrowserContext* context) {
  const char* kWootzWalletExtensionName = "Wootz Wallet";
  LOG(ERROR) << "JANGID: Searching for extension with name: "
             << kWootzWalletExtensionName;

  ExtensionRegistry* registry = ExtensionRegistry::Get(context);
  if (!registry) {
    LOG(ERROR) << "JANGID: Failed to get ExtensionRegistry";
    return ExtensionId();
  }

  LOG(ERROR) << "JANGID: Iterating through enabled extensions";
  int count = 0;
  for (const auto& extension : registry->enabled_extensions()) {
    count++;
    LOG(ERROR) << "JANGID: Checking extension " << count << ":";
    LOG(ERROR) << "  ID: " << extension->id();
    LOG(ERROR) << "  Name: " << extension->name();
    LOG(ERROR) << "  Version: " << extension->version().GetString();

    if (extension->name() == kWootzWalletExtensionName) {
      LOG(ERROR) << "JANGID: Found matching extension with ID: "
                 << extension->id();
      return extension->id();
    }
  }

  LOG(ERROR)<< "JANGID: No matching extension found. Total extensions checked: "<< count;
  
  return ExtensionId();
}

ExtensionFunction::ResponseAction WootzInfoFunction::Run() {
  const base::android::BuildInfo* build_info =
      base::android::BuildInfo::GetInstance();

  // Create a dictionary to store the relevant build information
  base::Value::Dict build_info_dict;
  build_info_dict.Set("device", build_info->device());
  build_info_dict.Set("manufacturer", build_info->manufacturer());
  build_info_dict.Set("model", build_info->model());
  build_info_dict.Set("brand", build_info->brand());
  build_info_dict.Set("sdk_int", build_info->sdk_int());
  build_info_dict.Set("android_build_id", build_info->android_build_id());
  build_info_dict.Set("android_build_fp", build_info->android_build_fp());
  build_info_dict.Set("gms_version_code", build_info->gms_version_code());
  build_info_dict.Set("host_package_name", build_info->host_package_name());
  build_info_dict.Set("package_name", build_info->package_name());
  build_info_dict.Set("package_version_code",
                      build_info->package_version_code());
  build_info_dict.Set("package_version_name",
                      build_info->package_version_name());
  build_info_dict.Set("abi_name", build_info->abi_name());
  build_info_dict.Set("is_tv", build_info->is_tv());
  build_info_dict.Set("is_automotive", build_info->is_automotive());
  build_info_dict.Set("is_foldable", build_info->is_foldable());

  std::string json_string;
  base::JSONWriter::Write(build_info_dict, &json_string);

  return RespondNow(WithArguments(json_string));
}

ExtensionFunction::ResponseAction WootzHelloWorldFunction::Run() {
  base::Value::Dict result;
  result.Set("message", "Hello, World!");

  std::string json_string;
  base::JSONWriter::Write(result, &json_string);

  return RespondNow(WithArguments(json_string));
}

ExtensionFunction::ResponseAction WootzLogFunction::Run() {
  if (args().size() < 1) {
    return RespondNow(NoArguments());
  }
  LOG(ERROR) << "CONSOLE.LOG: " << args()[0];
  return RespondNow(NoArguments());
}

[[maybe_unused]]
ExtensionFunction::ResponseAction WootzShowDialogFunction::Run() {
#if 0
    JNIEnv* env = base::android::AttachCurrentThread();
    
    content::WebContents* web_contents = GetSenderWebContents();
    if (!web_contents) {
        return RespondNow(Error("Unable to get WebContents"));
    }

    Java_WootzBridge_showDialog(env, web_contents->GetJavaWebContents());
#endif
    return RespondNow(NoArguments());
}

ExtensionFunction::ResponseAction WootzSetSelectedChainsFunction::Run() {
  if (args().empty() || !args()[0].is_list()) {
    return RespondNow(Error("Invalid arguments"));
  }

  const base::Value::List& chains_list = args()[0].GetList();
  std::vector<std::string> selected_chains;

  for (const auto& chain : chains_list) {
    if (!chain.is_string()) {
      continue;
    }
    const std::string& chain_str = chain.GetString();
    if (chain_str == "ethereum" || chain_str == "solana") {
      selected_chains.push_back(chain_str);
    }
  }

  // Ensure at least one chain is selected
  if (selected_chains.empty()) {
    return RespondNow(Error("At least one chain must be selected"));
  }

  auto* keyring_service = GetKeyringService(browser_context());
  if (!keyring_service) {
    return RespondNow(Error("KeyringService not available"));
  }

  keyring_service->SetSelectedChains(selected_chains);

  base::Value::Dict result;
  result.Set("success", true);
  return RespondNow(WithArguments(std::move(result)));
}

ExtensionFunction::ResponseAction WootzCreateWalletFunction::Run() {
  if (args().empty() || !args()[0].is_string()) {
    return RespondNow(Error("Invalid arguments"));
  }

  std::string password = args()[0].GetString();
  if (password.empty()) {
    return RespondNow(Error("Password cannot be empty"));
  }
  auto* keyring_service = GetKeyringService(browser_context());

  keyring_service->SetPassword(password);
    
    LOG(ERROR)<<"JANGID: Password"<<password;
    
  if (!keyring_service) {
    return RespondNow(Error("KeyringService not available"));
  }

  keyring_service->CreateWallet(
      password,
      base::BindOnce(&WootzCreateWalletFunction::OnWalletCreated, this));

  return RespondLater();
}

void WootzCreateWalletFunction::OnWalletCreated(
    const std::optional<std::string>& recovery_phrase) {
  base::Value::Dict result;
  result.Set("success", recovery_phrase.has_value());
  if (recovery_phrase) {
    result.Set("recoveryPhrase", *recovery_phrase);
  } else {
    result.Set("error", "Failed to create wallet");
  }

  base::Value::List result_list;
  result_list.Append(std::move(result));
  Respond(ArgumentList(std::move(result_list)));
}

ExtensionFunction::ResponseAction WootzUnlockWalletFunction::Run() {
  if (args().empty() || !args()[0].is_string()) {
    return RespondNow(Error("Invalid arguments"));
  }

  auto* keyring_service = GetKeyringService(browser_context());

  if (!keyring_service) {
    return RespondNow(Error("KeyringService not available"));
  }

   std::string input_password = args()[0].GetString();

  keyring_service->Unlock(
      input_password, base::BindOnce(&WootzUnlockWalletFunction::OnUnlocked, this));

  // Set the password again after unlocking
  keyring_service->SetPassword(input_password);

  return RespondLater();
}

void WootzUnlockWalletFunction::OnUnlocked(bool success) {
  base::Value::Dict result;
  result.Set("success", success);
  if (!success) {
    result.Set("error", "Failed to unlock wallet");
  }

  base::Value::List result_list;
  result_list.Append(std::move(result));
  Respond(ArgumentList(std::move(result_list)));
}

ExtensionFunction::ResponseAction WootzLockWalletFunction::Run() {
  auto* keyring_service = GetKeyringService(browser_context());

  if (!keyring_service) {
    return RespondNow(Error("KeyringService not available"));
  }

  keyring_service->Lock();
  
  base::Value::Dict result;
  result.Set("success", true);

  return RespondNow(WithArguments(std::move(result)));
}

ExtensionFunction::ResponseAction WootzIsLockedFunction::Run() {
  auto* keyring_service = GetKeyringService(browser_context());

  if (!keyring_service) {
    return RespondNow(Error("KeyringService not available"));
  }

  keyring_service->IsLocked(
      base::BindOnce(&WootzIsLockedFunction::OnIsLocked, this));

  return RespondLater();
}

void WootzIsLockedFunction::OnIsLocked(bool is_locked) {
  base::Value::Dict result;
  result.Set("isLocked", is_locked);

  base::Value::List result_list;
  result_list.Append(std::move(result));
  Respond(ArgumentList(std::move(result_list)));
}


ExtensionFunction::ResponseAction WootzGetAllAccountsFunction::Run() {
  auto* keyring_service = GetKeyringService(browser_context());

  if (!keyring_service) {
    return RespondNow(Error("KeyringService not available"));
  }

  keyring_service->GetAllAccounts(
      base::BindOnce(&WootzGetAllAccountsFunction::OnGetAllAccounts,
                     weak_factory_.GetWeakPtr()));

  return RespondLater();
}

void WootzGetAllAccountsFunction::OnGetAllAccounts(
    wootz_wallet::mojom::AllAccountsInfoPtr all_accounts_info) {

  base::Value::Dict result;
  base::Value::List accounts_list;

  for (const auto& account : all_accounts_info->accounts) {
    base::Value::Dict account_dict;
    account_dict.Set("address", account->address);
    account_dict.Set("name", account->name);
    account_dict.Set("coin", static_cast<int>(account->account_id->coin));
    accounts_list.Append(std::move(account_dict));
  }

  result.Set("accounts", std::move(accounts_list));
  result.Set("success", true);

  base::Value::List result_list;
  result_list.Append(std::move(result));
  Respond(ArgumentList(std::move(result_list)));
}

// static

void WootzSignMessageFunction::NotifyExtensionOfPendingRequest(
    content::BrowserContext* context) {

  auto* service = wootz_wallet::WootzWalletServiceFactory::GetServiceForContext(
      Profile::FromBrowserContext(context));

  if (!service) {
    LOG(ERROR) << "JANGID: NotifyExtensionOfPendingRequest: Failed to get WootzWalletService";
    return;
  }

  service->GetPendingSignMessageRequests(
      base::BindOnce(&WootzSignMessageFunction::OnGetPendingRequests,
                     base::Unretained(context)));
}

// static
void WootzSignMessageFunction::OnGetPendingRequests(
    content::BrowserContext* context,
    std::vector<wootz_wallet::mojom::SignMessageRequestPtr> requests) {
  if (requests.empty()) {
    LOG(ERROR) << "JANGID: OnGetPendingRequests: No pending requests, returning";
    return;
  }

  const auto& request = requests[0];
  base::Value::Dict request_dict;
  request_dict.Set("id", request->id);
  request_dict.Set("address", request->account_id->address);
  request_dict.Set("origin", request->origin_info->origin_spec);
  request_dict.Set("chainId", request->chain_id);
  request_dict.Set("isEip712", request->sign_data->is_eth_sign_typed_data());

  // Log the request to console
  std::string json_string;
  base::JSONWriter::Write(request_dict, &json_string);
  LOG(ERROR) << "JANGID: OnGetPendingRequests: Pending sign message request: "
             << json_string;

  // Pass the request to the extension using an event
  auto* event_router = EventRouter::Get(context);
  if (event_router) {
    LOG(ERROR) << "JANGID: OnGetPendingRequests: Broadcasting event to extension";
    base::Value::List event_args;
    event_args.Append(std::move(request_dict));

    // Fetch the Wootz Wallet extension ID
    ExtensionId wootz_extension_id = GetWootzWalletExtensionId(context);

    LOG(ERROR) << "JANGID: Wootz Wallet extension ID: " << wootz_extension_id;

    if (!wootz_extension_id.empty()) {
      // Get more information about the extension
      ExtensionRegistry* registry = ExtensionRegistry::Get(context);
      const Extension* extension = registry->GetExtensionById(
          wootz_extension_id, ExtensionRegistry::ENABLED);

      if (extension) {
        LOG(ERROR) << "JANGID: Extension details:";
        LOG(ERROR) << "  Name: " << extension->name();
        LOG(ERROR) << "  Version: " << extension->version().GetString();
        LOG(ERROR) << "  Description: " << extension->description();
      } else {
        LOG(ERROR) << "JANGID: Extension found but details not available";
      }

      std::unique_ptr<Event> event = std::make_unique<Event>(
          events::WOOTZ_ON_SIGN_MESSAGE_REQUESTED,
          "wootz.onSignMessageRequested",
          std::move(event_args), 
          context,
          std::nullopt,
          GURL(), 
          EventRouter::USER_GESTURE_UNKNOWN,
          mojom::EventFilteringInfo::New());

      LOG(ERROR) << "JANGID: Dispatching event to extension: "
                 << wootz_extension_id;
      event_router->DispatchEventToExtension(wootz_extension_id,
                                             std::move(event));

      
      // For opening Extension Automatically
      OpenExtensionsById(wootz_extension_id);
      
      LOG(ERROR) << "JANGID: Event dispatched successfully";
    } else {
      LOG(ERROR) << "JANGID: Wootz Wallet extension ID not found";

      // Log all enabled extensions
      ExtensionRegistry* registry = ExtensionRegistry::Get(context);
      for (const auto& extension : registry->enabled_extensions()) {
        LOG(ERROR) << "  ID: " << extension->id()
                   << ", Name: " << extension->name();
      }
    }
  } else {
    LOG(ERROR) << "JANGID: OnGetPendingRequests: Failed to get EventRouter";
  }
}
ExtensionFunction::ResponseAction WootzSignMessageFunction::Run() {

  // Check if we have the correct number of arguments
  if (args().size() < 2 || args().size() > 3) {
    return RespondNow(Error("Incorrect number of arguments"));
  }

  // Validate argument types
  if (!args()[0].is_int() || !args()[1].is_bool()) {
    return RespondNow(Error("Invalid argument types"));
  }

  int request_id = args()[0].GetInt();
  bool approved = args()[1].GetBool();
  std::string signature;
  if (args().size() == 3 && args()[2].is_string()) {
    signature = args()[2].GetString();
  }

  auto* service = wootz_wallet::WootzWalletServiceFactory::GetServiceForContext(
      Profile::FromBrowserContext(browser_context()));

  if (!service) {
    return RespondNow(Error("WootzWalletService not available"));
  }

  wootz_wallet::mojom::ByteArrayStringUnionPtr signature_ptr = nullptr;

  std::optional<std::string> error =
      approved ? std::nullopt
               : std::make_optional<std::string>("User rejected");

  service->NotifySignMessageRequestProcessed(approved, request_id,
                                             std::move(signature_ptr), error);

  closeExtensionBottomSheet();

  return RespondNow(WithArguments(base::Value(true)));
}

// background worker

const char kWootzJobsListKey[] = "Chrome.Wootzapp.Jobs";
const char kWootzJobResultsKey[] = "Chrome.Wootzapp.JobsResult";

ExtensionFunction::ResponseAction WootzSetJobFunction::Run() {
  if (!args()[0].GetIfString())
    return RespondNow(Error("URL must be a string"));
  std::string url = *args()[0].GetIfString();

  auto prefs = android::shared_preferences::GetChromeSharedPreferences();
  
  std::string jobs_json = prefs.ReadString(kWootzJobsListKey, "[]");
  LOG(ERROR) << "WOOTZ JOBS: " << jobs_json;
  absl::optional<base::Value> parsed = base::JSONReader::Read(jobs_json);
  base::Value::List* jobs = parsed->GetIfList();
  if (!jobs) {
    jobs = new base::Value::List();
  }

  jobs->Append(url);

  std::string new_jobs_json;
  base::JSONWriter::Write(base::Value(std::move(*jobs)), &new_jobs_json);
  prefs.WriteString(kWootzJobsListKey, new_jobs_json);

  return RespondNow(NoArguments());
}

ExtensionFunction::ResponseAction WootzRemoveJobFunction::Run() {
  if (!args()[0].GetIfString())
    return RespondNow(Error("URL must be a string"));
  std::string url = *args()[0].GetIfString();

  auto prefs = android::shared_preferences::GetChromeSharedPreferences();
  
  std::string jobs_json = prefs.ReadString(kWootzJobsListKey, "[]");

  LOG(ERROR) << "WOOTZ JOBS: " << jobs_json;

  absl::optional<base::Value> parsed = base::JSONReader::Read(jobs_json);
  base::Value::List* jobs = parsed->GetIfList();
  if (!jobs) return RespondNow(NoArguments());

  for (auto it = jobs->begin(); it != jobs->end(); ) {
    if (it->GetIfString() && *it->GetIfString() == url) {
      it = jobs->erase(it);
    } else {
      ++it;
    }
  }

  std::string new_jobs_json;
  base::JSONWriter::Write(base::Value(std::move(*jobs)), &new_jobs_json);
  prefs.WriteString(kWootzJobsListKey, new_jobs_json);

  return RespondNow(NoArguments());
}

ExtensionFunction::ResponseAction WootzGetJobsFunction::Run() {
  auto prefs = android::shared_preferences::GetChromeSharedPreferences();
  std::string results_json = prefs.ReadString(kWootzJobResultsKey, "[]");
  
  absl::optional<base::Value> parsed = base::JSONReader::Read(results_json);
  if (!parsed || !parsed->is_list()) {
    // Return empty array rather than error
    base::Value::List empty;
    return RespondNow(WithArguments(base::Value(std::move(empty))));
  }

  return RespondNow(WithArguments(std::move(*parsed)));
}

ExtensionFunction::ResponseAction WootzListJobsFunction::Run() {
  auto prefs = android::shared_preferences::GetChromeSharedPreferences();
  std::string jobs_json = prefs.ReadString(kWootzJobsListKey, "[]");
  
  LOG(ERROR) << "WOOTZ JOBS LIST JSON: " << jobs_json;

  absl::optional<base::Value> parsed = base::JSONReader::Read(jobs_json);
  if (!parsed || !parsed->is_list()) {
    base::Value::List empty;
    return RespondNow(WithArguments(base::Value(std::move(empty))));
  }

  return RespondNow(WithArguments(std::move(*parsed)));
}

ExtensionFunction::ResponseAction WootzCleanJobsFunction::Run() {
  auto prefs = android::shared_preferences::GetChromeSharedPreferences();
  
  // Clear both jobs and results
  prefs.RemoveKey(kWootzJobsListKey);
  prefs.RemoveKey(kWootzJobResultsKey);

  return RespondNow(NoArguments());
}

// ExtensionFunction::ResponseAction
// WootzShowConsentDialogAndMaybeStartServiceFunction::Run() {
//   JNIEnv* env = base::android::AttachCurrentThread();

//     content::WebContents* web_contents = GetSenderWebContents();
//     if (!web_contents) {
//         return RespondNow(Error("Unable to get WebContents"));
//     }

//     // First, check if we already have consent
//     if (!Java_WootzBridge_hasUserConsent(env)) {
//         // If not, show the consent dialog
//         // auto* callback_ptr = new base::OnceCallback<void(bool)>(
//         //     base::BindOnce(&WootzShowConsentDialogAndMaybeStartServiceFunction::OnConsentResult,
//         //                    this));

//         Java_WootzBridge_showConsentDialog(env, reinterpret_cast<jlong>(this),
//                                            web_contents->GetJavaWebContents());
//         return RespondLater();
//     } else {
//         // If we already have consent, start the service directly
//         Java_WootzBridge_startBrowsingDataService(env);
//         return RespondNow(NoArguments());
//     }
// //   base::Value::Dict result;
// //   result.Set("message", "Consent dialog shown and service started");

// //   std::string json_string;
// //   base::JSONWriter::Write(result, &json_string);

// //   return RespondNow(WithArguments(json_string));
// }
// void WootzShowConsentDialogAndMaybeStartServiceFunction::OnConsentDialogResult(JNIEnv* env, jboolean consented) {
//     if (consented) {
//         Java_WootzBridge_startBrowsingDataService(env);
//     }
//     Respond(NoArguments());
// }

}  // namespace extensions

void JNI_WootzBridge_OnConsentResult(JNIEnv* env, jboolean consented){
  // Implement the consent result handling here
  LOG(INFO) << "DKT: Consent result: " << (consented ? "true" : "false");
}

// extern "C" JNIEXPORT void JNICALL
// Java_org_chromium_chrome_browser_extensions_WootzBridge_nativeOnConsentDialogResult(
//     JNIEnv* env,
//     jclass clazz,
//     jlong native_ptr,
//     jboolean consented) {
//   auto* function = reinterpret_cast<extensions::WootzShowConsentDialogAndMaybeStartServiceFunction*>(native_ptr);
//   function->OnConsentDialogResult(env, consented);
// }
