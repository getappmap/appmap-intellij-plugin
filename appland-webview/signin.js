import Vue from "vue";
import { default as plugin, VSidebarSignIn } from "@appland/components"; // eslint-disable-line import/no-named-default
import "@appland/diagrams/dist/style.css";
import MessagePublisher from "messagePublisher";
import vscode from "vsCodeBridge";

function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on("init", ({ page, data: initData }) => {
    const app = new Vue({
      el: "#app",
      render(h) {
        return h(VSidebarSignIn, {
          ref: "ui",
          props: initData,
        });
      },
      data() {
        return initData;
      },
      mounted() {
        let emailInput = document.querySelector("#email-input");
        if (emailInput) {
          let focusEventSent = false;
          emailInput.addEventListener("focus", () => {
            if (!focusEventSent) {
              focusEventSent = true;
              setTimeout(() => vscode.postMessage({ command: "email-input-focused" }), 250);
            }
          });
        }
      },
    });

    app.$on("sign-in", () => {
      vscode.postMessage({ command: "sign-in" });
    });
    app.$on("click-sign-in-link", (linkType) => {
      vscode.postMessage({ command: "click-sign-in-link", linkType });
    });
    app.$on("activate", (apiKey) => {
      vscode.postMessage({ command: "activate", apiKey });
    });
    app.$on("apply-org-config", () => {
      vscode.postMessage({ command: "apply-org-config" });
    });

    // React to the applied state the IDE pushes whenever the org config changes, regardless of where
    // it was applied from (this view, the settings page, startup). Registered once with `on` (not
    // `once`) so it keeps reacting to later changes.
    messages.on("apply-org-config", (response) => {
      if (response.applied && app.$refs.ui) app.$refs.ui.onOrgConfigApplied();
    });
  });

  vscode.postMessage({ command: "ready" });
}

Vue.use(plugin);
mountWebview();
