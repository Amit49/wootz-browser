// Copyright 2018 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Common custom element registration code for the various guest view
// containers.

var $CustomElementRegistry =
    require('safeMethods').SafeMethods.$CustomElementRegistry;
var $Element = require('safeMethods').SafeMethods.$Element;
var $EventTarget = require('safeMethods').SafeMethods.$EventTarget;
var $HTMLElement = require('safeMethods').SafeMethods.$HTMLElement;
var GuestViewContainer = require('guestViewContainer').GuestViewContainer;
var GuestViewInternalNatives = requireNative('guest_view_internal');
var IdGenerator = requireNative('id_generator');
var logging = requireNative('logging');

// Conceptually, these are methods on GuestViewContainerElement.prototype.
// However, since that is exposed to users, we only set these callbacks on
// the prototype temporarily during the custom element registration.
var customElementCallbacks = {
  connectedCallback: function() {
    var internal = privates(this).internal;
    if (!internal)
      return;

    internal.elementAttached = true;
    internal.willAttachElement();
    internal.onElementAttached();
  },

  attributeChangedCallback: function(name, oldValue, newValue) {
    var internal = privates(this).internal;
    if (!internal)
      return;

    // Let the changed attribute handle its own mutation.
    internal.attributes[name].maybeHandleMutation(oldValue, newValue);
  },

  disconnectedCallback: function() {
    var internal = privates(this).internal;
    if (!internal)
      return;

    internal.elementAttached = false;
    internal.internalInstanceId = 0;
    internal.guest.destroy();
    internal.onElementDetached();
  }
};

// Registers the guestview as a custom element.
// |containerElementType| is a GuestViewContainerElement (e.g. WebViewElement)
function registerElement(elementName, containerElementType) {
  GuestViewInternalNatives.AllowGuestViewElementDefinition(() => {
    // We set the lifecycle callbacks so that they're available during
    // registration. Once that's done, we'll delete them so developers cannot
    // call them and produce unexpected behaviour.
    GuestViewContainerElement.prototype.connectedCallback =
        customElementCallbacks.connectedCallback;
    GuestViewContainerElement.prototype.disconnectedCallback =
        customElementCallbacks.disconnectedCallback;
    GuestViewContainerElement.prototype.attributeChangedCallback =
        customElementCallbacks.attributeChangedCallback;

    $CustomElementRegistry.define(
        window.customElements, $String.toLowerCase(elementName),
        containerElementType);
    $Object.defineProperty(window, elementName, {
      value: containerElementType,
    });

    delete GuestViewContainerElement.prototype.connectedCallback;
    delete GuestViewContainerElement.prototype.disconnectedCallback;
    delete GuestViewContainerElement.prototype.attributeChangedCallback;

    // Now that |observedAttributes| has been retrieved, we can hide it from
    // user code as well.
    delete containerElementType.observedAttributes;
  });
}

// Forward public API methods from |containerElementType|'s prototype to their
// internal implementations. If the method is defined on |containerType|, we
// forward to that. Otherwise, we forward to the method on |internalApi|. For
// APIs in |promiseMethodDetails|, the forwarded API will return a Promise that
// resolves with the result of the API or rejects with the error that is
// produced if the callback parameter is not defined.
function forwardApiMethods(
    containerElementType, containerType, internalApi, methodNames,
    promiseMethodDetails) {
  var createContainerImplHandler = function(m) {
    return function(var_args) {
      var internal = privates(this).internal;
      return $Function.apply(internal[m], internal, arguments);
    };
  };

  // Add a version of the container handler function defined above which returns
  // a Promise.
  let createContainerImplPromiseHandler =
      function(m) {
    return function(var_args) {
      const internal = privates(this).internal;
      const args = [...arguments];
      if (args[m.callbackIndex] != undefined) {
        return $Function.apply(internal[m], internal, arguments);
      }
      return new $Promise.self((resolve, reject) => {
          const callback = function(result) {
            if (bindingUtil.hasLastError()) {
              reject(bindingUtil.getLastErrorMessage());
              bindingUtil.clearLastError();
              return;
            }
            resolve(result);
          };
          args[m.callbackIndex] = callback;
        $Function.apply(internal[m.name], internal, args);
      });
    }
  }

  var createInternalApiHandler = function(m) {
    return function(var_args) {
      var internal = privates(this).internal;
      var instanceId = internal.guest.getId();
      if (!instanceId) {
        return false;
      }
      var args = $Array.concat([instanceId], $Array.slice(arguments));
      $Function.apply(internalApi[m], null, args);
      return true;
    };
  };

  // Add a version of the internal handler function defined above which returns
  // a Promise.
  let createInternalApiPromiseHandler =
      function(m) {
    return function(var_args) {
      const internal = privates(this).internal;
      const instanceId = internal.guest.getId();
      var args = $Array.slice(arguments);
      if (args[m.callbackIndex] !== undefined) {
        if (!instanceId) {
          return false;
        }
        args = $Array.concat([instanceId], args);
        $Function.apply(internalApi[m.name], null, args)
        return true;
      }
      return new $Promise.self((resolve, reject) => {
        if (!instanceId) {
          reject();
          return;
        }
          const callback = function(result) {
            if (bindingUtil.hasLastError()) {
              reject(bindingUtil.getLastErrorMessage());
              bindingUtil.clearLastError();
              return;
            }
            resolve(result);
          };
          args[m.callbackIndex] = callback;
        args = $Array.concat([instanceId], args);
        $Function.apply(internalApi[m.name], null, args);
      });
    }
  }

  for (var m of methodNames) {
    if (!containerElementType.prototype[m]) {
      if (containerType.prototype[m]) {
        containerElementType.prototype[m] = createContainerImplHandler(m);
      } else if (internalApi && internalApi[m]) {
        containerElementType.prototype[m] = createInternalApiHandler(m);
      } else {
        logging.DCHECK(false, m + ' has no implementation.');
      }
    }
  }

  for (let m of promiseMethodDetails) {
    if (!containerElementType.prototype[m.name]) {
      if (containerType.prototype[m.name]) {
        containerElementType.prototype[m.name] =
            createContainerImplPromiseHandler(m);
      } else if (internalApi && internalApi[m.name]) {
        containerElementType.prototype[m.name] =
            createInternalApiPromiseHandler(m);
      } else {
        logging.DCHECK(false, m.name + 'has no implementation.');
      }
    }
  }
}

class GuestViewContainerElement extends HTMLElement {}

// Override |focus| to let |internal| handle it.
GuestViewContainerElement.prototype.focus = function() {
  var internal = privates(this).internal;
  if (!internal)
    return;

  internal.focus();
};

// Exports.
exports.$set('GuestViewContainerElement', GuestViewContainerElement);
exports.$set('registerElement', registerElement);
exports.$set('forwardApiMethods', forwardApiMethods);
