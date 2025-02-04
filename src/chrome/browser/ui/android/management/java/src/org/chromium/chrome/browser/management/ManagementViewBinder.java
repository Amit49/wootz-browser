// Copyright 2021 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.management;

import org.chromium.ui.modelutil.PropertyKey;
import org.chromium.ui.modelutil.PropertyModel;

/** View updater based on properties for ManagementPage. */
class ManagementViewBinder {
    /**
     * Listens to changes in MVC model.
     * @param model MVC property model to write changes to.
     * @param view Inflated view for the ManagementPage.
     * @param propertyKey Specific model attribute that changed on this event.
     */
    public static void bind(PropertyModel model, ManagementView view, PropertyKey propertyKey) {
        if (propertyKey == ManagementProperties.BROWSER_IS_MANAGED) {
            view.setManaged(model.get(ManagementProperties.BROWSER_IS_MANAGED));
        } else if (propertyKey == ManagementProperties.TITLE) {
            view.setTitleText(model.get(ManagementProperties.TITLE));
        } else if (propertyKey == ManagementProperties.LEARN_MORE_TEXT) {
            view.setLearnMoreText(model.get(ManagementProperties.LEARN_MORE_TEXT));
        } else if (propertyKey == ManagementProperties.REPORTING_IS_ENABLED) {
            view.setReportingEnabled(model.get(ManagementProperties.REPORTING_IS_ENABLED));
        } else if (propertyKey == ManagementProperties.LEGACY_TECH_REPORTING_IS_ENABLED) {
            view.setLegacyTechReportingEnabled(
                    model.get(ManagementProperties.LEGACY_TECH_REPORTING_IS_ENABLED));
        } else if (propertyKey == ManagementProperties.LEGACY_TECH_REPORTING_TEXT) {
            view.setLegacyTechReportingText(
                    model.get(ManagementProperties.LEGACY_TECH_REPORTING_TEXT));
        }
    }
}
