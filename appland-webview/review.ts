import Vue from 'vue';

import { default as plugin, VReview, ReviewBackend } from '@appland/components';

import MessagePublisher from 'messagePublisher';
import vscode from 'vsCodeBridge';

// if you want to use static data for development,
// import REVIEW from JSON to skip calling the RPC
// import REVIEW from './review.json';
const REVIEW = undefined;

Vue.use(plugin)

const EVENTS = ['open-location', 'show-navie-thread'];
const messages = new MessagePublisher(vscode);

messages.on('init', ({ rpcPort, baseRef }) => {
  const app = new Vue({
    el: '#app',
    render: (h) =>
      h(VReview, {
        ref: 'review',
      }),
  });

  const reviewComponent = app.$refs.review as VReview;

  for (const event of EVENTS) app.$on(event, (...args) => vscode.postMessage({command: event, args}));

  const backend = new ReviewBackend(reviewComponent, { rpcPort, ...REVIEW });
  if (!REVIEW) backend.startReview(baseRef);
});


vscode.postMessage({ command: 'ready' });
