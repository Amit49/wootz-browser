// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'chrome-untrusted://lens/selection_overlay.js';

import type {Point, RectF} from '//resources/mojo/ui/gfx/geometry/mojom/geometry.mojom-webui.js';
import {BrowserProxyImpl} from 'chrome-untrusted://lens/browser_proxy.js';
import {CenterRotatedBox_CoordinateType} from 'chrome-untrusted://lens/geometry.mojom-webui.js';
import type {CenterRotatedBox} from 'chrome-untrusted://lens/geometry.mojom-webui.js';
import type {SelectionOverlayElement} from 'chrome-untrusted://lens/selection_overlay.js';
import {loadTimeData} from 'chrome-untrusted://resources/js/load_time_data.js';
import {assertDeepEquals, assertEquals} from 'chrome-untrusted://webui-test/chai_assert.js';
import {waitAfterNextRender} from 'chrome-untrusted://webui-test/polymer_test_util.js';

import {getImageBoundingRect, simulateClick, simulateDrag} from '../utils/selection_utils.js';

import {TestLensOverlayBrowserProxy} from './test_overlay_browser_proxy.js';

const TAP_REGION_WIDTH = 300;
const TAP_REGION_HEIGHT = 300;

suite('ManualRegionSelection', function() {
  let testBrowserProxy: TestLensOverlayBrowserProxy;
  let selectionOverlayElement: SelectionOverlayElement;

  setup(() => {
    // Resetting the HTML needs to be the first thing we do in setup to
    // guarantee that any singleton instances don't change while any UI is still
    // attached to the DOM.
    document.body.innerHTML = window.trustedTypes!.emptyHTML;

    testBrowserProxy = new TestLensOverlayBrowserProxy();
    BrowserProxyImpl.setInstance(testBrowserProxy);

    // Set load time values here so we can test the UI properly.
    loadTimeData.overrideValues({
      ['tapRegionWidth']: TAP_REGION_WIDTH,
      ['tapRegionHeight']: TAP_REGION_HEIGHT,
    });

    // Turn off the shimmer. Since the shimmer is resource intensive, turn off
    // to prevent from causing issues in the tests.
    loadTimeData.overrideValues({'enableShimmer': false});

    selectionOverlayElement = document.createElement('lens-selection-overlay');
    document.body.appendChild(selectionOverlayElement);

    // Set image to be less than fullscreen so we can handle logic of drag
    // ending off this element.
    selectionOverlayElement.$.backgroundImage.style.height =
        'calc(100vh - 100px)';
    selectionOverlayElement.$.backgroundImage.style.width =
        'calc(100vw - 100px)';

    return waitAfterNextRender(selectionOverlayElement);
  });

  // Normalizes the given values to the size of selection overlay.
  function normalizedBox(box: RectF): RectF {
    const boundingRect = getImageBoundingRect(selectionOverlayElement);
    return {
      x: box.x / boundingRect.width,
      y: box.y / boundingRect.height,
      width: box.width / boundingRect.width,
      height: box.height / boundingRect.height,
    };
  }

  // Does a drag and verifies that expectedRect is sent via mojo.
  async function assertDragGestureSendsRequest(
      fromPoint: Point, toPoint: Point, expectedRect: CenterRotatedBox) {
    // Ensures the whenCalled method returns because of our drag, not a leftover
    // call that already happened.
    testBrowserProxy.handler.resetResolver('issueLensRequest');

    await simulateDrag(selectionOverlayElement, fromPoint, toPoint);
    const rect = await testBrowserProxy.handler.whenCalled('issueLensRequest');
    assertDeepEquals(expectedRect, rect);
  }

  // Does a click and verifies that expectedRect is sent via mojo.
  async function assertClickSendsRequest(
      point: Point, expectedRect: CenterRotatedBox) {
    // Ensures the whenCalled method returns because of our drag, not a leftover
    // call that already happened.
    testBrowserProxy.handler.resetResolver('issueLensRequest');

    await simulateClick(selectionOverlayElement, point);
    const rect = await testBrowserProxy.handler.whenCalled('issueLensRequest');
    assertDeepEquals(expectedRect, rect);
  }

  test('ClickShowsRegion', async () => {
    const imageBounds = getImageBoundingRect(selectionOverlayElement);
    const pointInOverlay = {
      x: imageBounds.left + TAP_REGION_WIDTH / 2,
      y: imageBounds.top + TAP_REGION_HEIGHT / 2,
    };

    const expectedRect: CenterRotatedBox = {
      box: normalizedBox({
        x: TAP_REGION_WIDTH / 2,
        y: TAP_REGION_HEIGHT / 2,
        width: TAP_REGION_WIDTH,
        height: TAP_REGION_HEIGHT,
      }),
      rotation: 0,
      coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
    };
    await assertClickSendsRequest(pointInOverlay, expectedRect);
  });

  test('ClickShowsRegionOverlayBiggerThanTapRegion', async () => {
    selectionOverlayElement.$.backgroundImage.style.display = 'block';
    selectionOverlayElement.$.backgroundImage.style.width = '100px';
    selectionOverlayElement.$.backgroundImage.style.height = '100px';
    await waitAfterNextRender(selectionOverlayElement);

    const imageBounds = getImageBoundingRect(selectionOverlayElement);
    const pointInOverlay = {
      x: imageBounds.left,
      y: imageBounds.top,
    };

    // The expected rect should be the entire canvas.
    const expectedRect: CenterRotatedBox = {
      box: {
        x: 0.5,
        y: 0.5,
        width: 1,
        height: 1,
      },
      rotation: 0,
      coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
    };
    await assertClickSendsRequest(pointInOverlay, expectedRect);
  });

  test('ClickShowsRegionWithCenterClampedToRegionWidthTopLeft', async () => {
    const imageBounds = getImageBoundingRect(selectionOverlayElement);
    const pointInOverlay = {
      x: imageBounds.left + (TAP_REGION_WIDTH / 4),
      y: imageBounds.top + (TAP_REGION_HEIGHT / 4),
    };

    const expectedRect: CenterRotatedBox = {
      box: normalizedBox({
        x: TAP_REGION_WIDTH / 2,
        y: TAP_REGION_HEIGHT / 2,
        width: TAP_REGION_WIDTH,
        height: TAP_REGION_HEIGHT,
      }),
      rotation: 0,
      coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
    };
    await assertClickSendsRequest(pointInOverlay, expectedRect);
  });

  test('ClickShowsRegionWithCenterClampedToRegionWidthTopRight', async () => {
    const imageBounds = getImageBoundingRect(selectionOverlayElement);
    const pointInOverlay = {
      x: imageBounds.right - (TAP_REGION_WIDTH / 4),
      y: imageBounds.top + (TAP_REGION_HEIGHT / 4),
    };

    const expectedRect: CenterRotatedBox = {
      box: normalizedBox({
        x: imageBounds.width - TAP_REGION_WIDTH / 2,
        y: TAP_REGION_HEIGHT / 2,
        width: TAP_REGION_WIDTH,
        height: TAP_REGION_HEIGHT,
      }),
      rotation: 0,
      coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
    };
    await assertClickSendsRequest(pointInOverlay, expectedRect);
  });

  test('ClickShowsRegionWithCenterClampedToRegionWidthBottomLeft', async () => {
    const imageBounds = getImageBoundingRect(selectionOverlayElement);
    const pointInOverlay = {
      x: imageBounds.left + (TAP_REGION_WIDTH / 4),
      y: imageBounds.bottom - (TAP_REGION_HEIGHT / 4),
    };

    const expectedRect: CenterRotatedBox = {
      box: normalizedBox({
        x: TAP_REGION_WIDTH / 2,
        y: imageBounds.height - TAP_REGION_HEIGHT / 2,
        width: TAP_REGION_WIDTH,
        height: TAP_REGION_HEIGHT,
      }),
      rotation: 0,
      coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
    };
    await assertClickSendsRequest(pointInOverlay, expectedRect);
  });

  test(
      'ClickShowsRegionWithCenterClampedToRegionWidthBottomRight', async () => {
        const imageBounds = getImageBoundingRect(selectionOverlayElement);
        const pointInOverlay = {
          x: imageBounds.right - (TAP_REGION_WIDTH / 4),
          y: imageBounds.bottom - (TAP_REGION_HEIGHT / 4),
        };

        const expectedRect: CenterRotatedBox = {
          box: normalizedBox({
            x: imageBounds.width - TAP_REGION_WIDTH / 2,
            y: imageBounds.height - TAP_REGION_HEIGHT / 2,
            width: TAP_REGION_WIDTH,
            height: TAP_REGION_HEIGHT,
          }),
          rotation: 0,
          coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
        };
        await assertClickSendsRequest(pointInOverlay, expectedRect);
      });

  test(
      `verify that completing a drag within the overlay bounds issues correct
      lens request via mojo`,
      async () => {
        const imageBounds = getImageBoundingRect(selectionOverlayElement);
        const startPointInsideOverlay = {
          x: imageBounds.left + 10,
          y: imageBounds.top + 10,
        };
        const endPointInsideOverlay = {
          x: imageBounds.left + 100,
          y: imageBounds.top + 100,
        };

        const expectedRect: CenterRotatedBox = {
          box: normalizedBox({x: 55, y: 55, width: 90, height: 90}),
          rotation: 0,
          coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
        };
        await assertDragGestureSendsRequest(
            startPointInsideOverlay, endPointInsideOverlay, expectedRect);
      });

  test(
      'verify that completing a drag above the selection overlay rounds y to 0',
      async () => {
        const imageBounds = getImageBoundingRect(selectionOverlayElement);
        const startPointInsideOverlay = {
          x: imageBounds.left + 10,
          y: imageBounds.top + 10,
        };
        const endPointAboveOverlay = {
          x: imageBounds.left + 100,
          y: imageBounds.top - 30,
        };

        const expectedRect: CenterRotatedBox = {
          box: normalizedBox({x: 55, y: 5, width: 90, height: 10}),
          rotation: 0,
          coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
        };
        await assertDragGestureSendsRequest(
            startPointInsideOverlay, endPointAboveOverlay, expectedRect);
      });

  test(
      `verify that completing a drag below the selection overlay rounds y to
      overlay height`,
      async () => {
        const imageBounds = getImageBoundingRect(selectionOverlayElement);
        const startPointInsideOverlay = {
          x: imageBounds.left + 10,
          y: imageBounds.bottom - 20,
        };
        const endPointBelowOverlay = {
          x: imageBounds.left + 100,
          y: imageBounds.bottom + 20,
        };

        const expectedRect: CenterRotatedBox = {
          box: normalizedBox(
              {x: 55, y: imageBounds.height - 10, width: 90, height: 20}),
          rotation: 0,
          coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
        };
        await assertDragGestureSendsRequest(
            startPointInsideOverlay, endPointBelowOverlay, expectedRect);
      });

  test(
      `verify that completing a drag to the left of the selection overlay rounds
       x to 0`,
      async () => {
        const imageBounds = getImageBoundingRect(selectionOverlayElement);
        const startPointInsideOverlay = {
          x: imageBounds.left + 20,
          y: imageBounds.top + 10,
        };
        const endPointLeftOfOverlay = {
          x: imageBounds.left - 10,
          y: imageBounds.top + 100,
        };

        const expectedRect: CenterRotatedBox = {
          box: normalizedBox({x: 10, y: 55, width: 20, height: 90}),
          rotation: 0,
          coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
        };
        await assertDragGestureSendsRequest(
            startPointInsideOverlay, endPointLeftOfOverlay, expectedRect);
      });

  test(
      `verify that completing a drag to the right of the selection overlay
      rounds x to overlay width`,
      async () => {
        const imageBounds = getImageBoundingRect(selectionOverlayElement);
        const startPointInsideOverlay = {
          x: imageBounds.right - 20,
          y: imageBounds.top + 10,
        };
        const endPointRightOfOverlay = {
          x: imageBounds.right + 10,
          y: imageBounds.top + 100,
        };

        const expectedRect: CenterRotatedBox = {
          box: normalizedBox(
              {x: imageBounds.width - 10, y: 55, width: 20, height: 90}),
          rotation: 0,
          coordinateType: CenterRotatedBox_CoordinateType.kNormalized,
        };
        await assertDragGestureSendsRequest(
            startPointInsideOverlay, endPointRightOfOverlay, expectedRect);
      });

  test('verify canvas resizes', async () => {
    selectionOverlayElement.$.regionSelectionLayer.setCanvasSizeTo(50, 50);
    await waitAfterNextRender(selectionOverlayElement.$.regionSelectionLayer);
    assertEquals(
        50,
        selectionOverlayElement.$.regionSelectionLayer.$.regionSelectionCanvas
            .width);
    assertEquals(
        50,
        selectionOverlayElement.$.regionSelectionLayer.$.regionSelectionCanvas
            .height);

    selectionOverlayElement.$.regionSelectionLayer.setCanvasSizeTo(100, 100);
    await waitAfterNextRender(selectionOverlayElement.$.regionSelectionLayer);
    assertEquals(
        100,
        selectionOverlayElement.$.regionSelectionLayer.$.regionSelectionCanvas
            .width);
    assertEquals(
        100,
        selectionOverlayElement.$.regionSelectionLayer.$.regionSelectionCanvas
            .height);
  });
});
