// Copyright 2012 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CHROME_BROWSER_EXTENSIONS_API_WOOTZ_WOOTZ_API_H_
#define CHROME_BROWSER_EXTENSIONS_API_WOOTZ_WOOTZ_API_H_

#include <set>
#include <string>

#include "base/memory/raw_ptr.h"
#include "base/scoped_observation.h"
#include "chrome/browser/extensions/extension_icon_manager.h"
#include "chrome/common/extensions/api/omnibox.h"
#include "components/search_engines/template_url_service.h"
#include "extensions/browser/browser_context_keyed_api_factory.h"
#include "extensions/browser/extension_function.h"
#include "extensions/browser/extension_function_histogram_value.h"
#include "extensions/browser/extension_registry.h"
#include "extensions/browser/extension_registry_observer.h"
#include "extensions/common/extension_id.h"
#include "ui/base/window_open_disposition.h"
#include "components/wootz_wallet/common/wootz_wallet.mojom.h"
#include "base/android/jni_array.h"
#include "base/android/jni_string.h"

class Profile;

namespace content {
class BrowserContext;
class WebContents;
}

namespace extensions {
class WootzInfoFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.info", WOOTZ_INFO)

  WootzInfoFunction() = default;

  WootzInfoFunction(
      const WootzInfoFunction&) = delete;
  WootzInfoFunction& operator=(
      const WootzInfoFunction&) = delete;

 protected:
  ~WootzInfoFunction() override {}

  ResponseAction Run() override;
};

class WootzHelloWorldFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.helloWorld", WOOTZ_HELLOWORLD)

  WootzHelloWorldFunction() = default;

  WootzHelloWorldFunction(const WootzHelloWorldFunction&) = delete;
  WootzHelloWorldFunction& operator=(const WootzHelloWorldFunction&) = delete;

 protected:
  ~WootzHelloWorldFunction() override {}

  ResponseAction Run() override;
};

class WootzShowDialogFunction : public ExtensionFunction {
    DECLARE_EXTENSION_FUNCTION("wootz.showDialog", WOOTZ_SHOWDIALOG)
    WootzShowDialogFunction() = default;

    WootzShowDialogFunction(const WootzShowDialogFunction&) = delete;
    WootzShowDialogFunction& operator=(const WootzShowDialogFunction&) = delete;

  protected:
    ~WootzShowDialogFunction() override {}

    ResponseAction Run() override;
};

class WootzLogFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.log", WOOTZ_LOG)
  WootzLogFunction() = default;

  WootzLogFunction(
      const WootzLogFunction&) = delete;
  WootzLogFunction& operator=(
      const WootzLogFunction&) = delete;

 protected:
  ~WootzLogFunction() override {}

  ResponseAction Run() override;
};

class WootzSetSelectedChainsFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.setSelectedChains", WOOTZ_SELECT_CHAIN)
  WootzSetSelectedChainsFunction() = default;

 protected:
  ~WootzSetSelectedChainsFunction() override = default;
  ResponseAction Run() override;
};

class WootzCreateWalletFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.createWallet", WOOTZ_CREATE_WALLET)
 protected:
  ~WootzCreateWalletFunction() override {}
  ResponseAction Run() override;
 private:
  void OnWalletCreated(const std::optional<std::string>& recovery_phrase);
};

class WootzIsWalletCreatedFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.isWalletCreated", WOOTZ_IS_WALLET_CREATED)
 protected:
  ~WootzIsWalletCreatedFunction() override {}
  ResponseAction Run() override;
};

class WootzUnlockWalletFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.unlockWallet", WOOTZ_UNLOCK_WALLET)
 protected:
  ~WootzUnlockWalletFunction() override {}
  ResponseAction Run() override;
 private:
  void OnUnlocked(bool success);
};

class WootzLockWalletFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.lockWallet", WOOTZ_LOCK_WALLET)
 protected:
  ~WootzLockWalletFunction() override {}
  ResponseAction Run() override;
};

class WootzIsLockedFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.isLocked", WOOTZ_IS_LOCKED)
 protected:
  ~WootzIsLockedFunction() override {}
  ResponseAction Run() override;
 private:
  void OnIsLocked(bool is_locked);
};

class WootzGetAllAccountsFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.getAllAccounts", WOOTZ_GET_ALL_ACCOUNTS)
 protected:
  ~WootzGetAllAccountsFunction() override {}
  ResponseAction Run() override;

 private:
  void OnGetAllAccounts(wootz_wallet::mojom::AllAccountsInfoPtr all_accounts_info);
  
  base::WeakPtrFactory<WootzGetAllAccountsFunction> weak_factory_{this};
};

class WootzSignMessageFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.signMessage", WOOTZ_SIGN_MESSAGE)
  
  static void NotifyExtensionOfPendingRequest(content::BrowserContext* context);

 private:
  ~WootzSignMessageFunction() override {}
  ResponseAction Run() override;
  static void OnGetPendingRequests(
      content::BrowserContext* context,
      std::vector<wootz_wallet::mojom::SignMessageRequestPtr> requests);
};
// background service api
class WootzSetJobFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.setJob", WOOTZ_SETJOB)
  WootzSetJobFunction() = default;
  WootzSetJobFunction(const WootzSetJobFunction&) = delete;
  WootzSetJobFunction& operator=(const WootzSetJobFunction&) = delete;

 protected:
  ~WootzSetJobFunction() override = default;
  ResponseAction Run() override;
};

class WootzRemoveJobFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.removeJob", WOOTZ_REMOVEJOB)
  WootzRemoveJobFunction() = default;
  WootzRemoveJobFunction(const WootzRemoveJobFunction&) = delete;
  WootzRemoveJobFunction& operator=(const WootzRemoveJobFunction&) = delete;

 protected:
  ~WootzRemoveJobFunction() override = default;
  ResponseAction Run() override;
};

class WootzGetJobsFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("wootz.getJobs", WOOTZ_GETJOBS)
  WootzGetJobsFunction() = default;
  WootzGetJobsFunction(const WootzGetJobsFunction&) = delete;
  WootzGetJobsFunction& operator=(const WootzGetJobsFunction&) = delete;

 protected:
  ~WootzGetJobsFunction() override = default;
  ResponseAction Run() override;
};

class WootzListJobsFunction : public ExtensionFunction {
public:
 DECLARE_EXTENSION_FUNCTION("wootz.listJobs", WOOTZ_LISTJOBS)

 WootzListJobsFunction() = default;

 WootzListJobsFunction(const WootzListJobsFunction&) = delete;
 WootzListJobsFunction& operator=(const WootzListJobsFunction&) = delete;

protected:
 ~WootzListJobsFunction() override {}

 ResponseAction Run() override;
};

class WootzCleanJobsFunction : public ExtensionFunction {
public:
 DECLARE_EXTENSION_FUNCTION("wootz.cleanJobs", WOOTZ_CLEANJOBS)

 WootzCleanJobsFunction() = default;

 WootzCleanJobsFunction(const WootzCleanJobsFunction&) = delete;
 WootzCleanJobsFunction& operator=(const WootzCleanJobsFunction&) = delete;

protected:
 ~WootzCleanJobsFunction() override {}

 ResponseAction Run() override;
};


}  // namespace extensions
#endif  // CHROME_BROWSER_EXTENSIONS_API_WOOTZ_WOOTZ_API_H_
