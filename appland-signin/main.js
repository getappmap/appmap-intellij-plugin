import Vue from 'vue';
import { default as plugin } from '@appland/components'; // eslint-disable-line import/no-named-default

export {
  mountWebview
} from './webview';

Vue.use(plugin);
