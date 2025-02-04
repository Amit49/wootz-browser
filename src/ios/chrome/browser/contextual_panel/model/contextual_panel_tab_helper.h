// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef IOS_CHROME_BROWSER_CONTEXTUAL_PANEL_MODEL_CONTEXTUAL_PANEL_TAB_HELPER_H_
#define IOS_CHROME_BROWSER_CONTEXTUAL_PANEL_MODEL_CONTEXTUAL_PANEL_TAB_HELPER_H_

#include "base/memory/weak_ptr.h"
#import "base/observer_list.h"
#include "base/scoped_observation.h"
#import "ios/chrome/browser/contextual_panel/model/contextual_panel_item_configuration.h"
#import "ios/web/public/web_state_observer.h"
#import "ios/web/public/web_state_user_data.h"

enum class ContextualPanelItemType;
class ContextualPanelModel;
struct ContextualPanelItemConfiguration;
class ContextualPanelTabHelperObserver;

// Tab helper controlling the Contextual Panel feature for a given tab.
class ContextualPanelTabHelper
    : public web::WebStateObserver,
      public web::WebStateUserData<ContextualPanelTabHelper> {
 public:
  ContextualPanelTabHelper(const ContextualPanelTabHelper&) = delete;
  ContextualPanelTabHelper& operator=(const ContextualPanelTabHelper&) = delete;

  ~ContextualPanelTabHelper() override;

  // Adds and removes observers for contextual panel actions. The order in
  // which notifications are sent to observers is undefined. Clients must be
  // sure to remove the observer before they go away.
  void AddObserver(ContextualPanelTabHelperObserver* observer);
  void RemoveObserver(ContextualPanelTabHelperObserver* observer);

  // Whether there exists at least one finalized Contextual Panel model config
  // currently available in the cached list of sorted configs. This will be
  // false before all the models have returned a response or timed out.
  bool HasCachedConfigsAvailable();

  // Returns a list of the finalized Contextual Panel model configs
  // currently available in the cached list of sorted configs.
  std::vector<base::WeakPtr<ContextualPanelItemConfiguration>>
  GetCurrentCachedConfigurations();

  // Gets the first config in the cached list of sorted Contextual Panel model
  // configs.
  base::WeakPtr<ContextualPanelItemConfiguration> GetFirstCachedConfig();

  // Getter and setter for is_contextual_panel_currently_opened_.
  bool IsContextualPanelCurrentlyOpened();
  void SetContextualPanelCurrentlyOpened(bool opened);

  // Getter and setter for large_entrypoint_shown_for_curent_page_navigation_.
  bool WasLargeEntrypointShown();
  void SetLargeEntrypointShown(bool shown);

  // WebStateObserver:
  void DidStartNavigation(web::WebState* web_state,
                          web::NavigationContext* navigation_context) override;
  void DidFinishNavigation(web::WebState* web_state,
                           web::NavigationContext* navigation_context) override;
  void WebStateDestroyed(web::WebState* web_state) override;
  void PageLoaded(
      web::WebState* web_state,
      web::PageLoadCompletionStatus load_completion_status) override;

 private:
  friend class web::WebStateUserData<ContextualPanelTabHelper>;

  // Helper struct to store responses received from individual models.
  struct ModelResponse {
    bool completed = false;
    std::unique_ptr<ContextualPanelItemConfiguration> configuration = nullptr;

    // Constructs a non-complete response.
    ModelResponse();

    ModelResponse(const ModelResponse&) = delete;
    ModelResponse& operator=(const ModelResponse&) = delete;
    ModelResponse& operator=(ModelResponse&& other) = default;

    // Constructs a completed response with the provided configuration
    explicit ModelResponse(
        std::unique_ptr<ContextualPanelItemConfiguration>&& configuration);
    ~ModelResponse();
  };

  ContextualPanelTabHelper(
      web::WebState* web_state,
      std::map<ContextualPanelItemType, raw_ptr<ContextualPanelModel>> models);

  // Callback for when the given model has finished fetching its data.
  void ModelCallbackReceived(
      ContextualPanelItemType item_type,
      std::unique_ptr<ContextualPanelItemConfiguration> configuration);

  // Query all the individual models for their data.
  void QueryModels();

  // Do any necessary work after all requests are completed or time out.
  void AllRequestsFinished();

  WEB_STATE_USER_DATA_KEY_DECL();

  // Whether the Contextual Panel is currently opened for the current tab.
  bool is_contextual_panel_currently_opened_ = false;

  // Whether the large Contextual Panel entrypoint has been shown for the
  // current navigation.
  bool large_entrypoint_shown_for_curent_page_navigation_ = false;

  // The WebState this instance is observing. Will be null after
  // WebStateDestroyed has been called.
  raw_ptr<web::WebState> web_state_ = nullptr;

  // Map of the models this tab helper should query for possible panels.
  std::map<ContextualPanelItemType, raw_ptr<ContextualPanelModel>> models_;

  // Holds the responses currently being returned.
  std::map<ContextualPanelItemType, ModelResponse> responses_;

  // Holds the current finalized and sorted list of configurations passed to
  // observers when all requests have completed. Not the source of truth of
  // panel model responses, simply a cached list of their configs.
  std::vector<base::WeakPtr<ContextualPanelItemConfiguration>>
      sorted_weak_configurations_;

  // List of observers to be notified when the Contextual Panel gets new data.
  base::ObserverList<ContextualPanelTabHelperObserver, true> observers_;

  // Scoped observation for WebState.
  base::ScopedObservation<web::WebState, web::WebStateObserver>
      web_state_observation_{this};

  base::WeakPtrFactory<ContextualPanelTabHelper> weak_ptr_factory_;
};

#endif  // IOS_CHROME_BROWSER_CONTEXTUAL_PANEL_MODEL_CONTEXTUAL_PANEL_TAB_HELPER_H_
