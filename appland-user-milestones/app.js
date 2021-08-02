import Vue from 'vue';
import { default as plugin } from '@appland/components'; // eslint-disable-line import/no-named-default

export { mountQuickstartInstallAgent, mountQuickstartOpenAppmaps } from './quickstartDocsView';

Vue.use(plugin);
