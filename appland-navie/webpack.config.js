/* eslint-disable */
const path = require('path');
const webpack = require('webpack');

module.exports = (env, argv) => {
  let config = {
    entry: './main.js',
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: 'main.js',
      devtoolModuleFilenameTemplate: '[resource-path]',
      library: 'AppLandWeb',
    },
    resolve: {
      extensions: ['.js', '.mjs'],
      alias: {
        messagePublisher: path.resolve('../appland-shared/messagePublisher.js'),
        vsCodeBridge: path.resolve('../appland-shared/vsCodeBridge.js'),
        handleAppMapMessages: path.resolve('../appland-shared/handleAppMapMessages.js'),
        vue: path.resolve('./node_modules/vue/dist/vue.esm.browser.min.js'),
        vuex: path.resolve('./node_modules/vuex'),
        'socket.io-client': path.resolve('./node_modules/socket.io-client/dist/socket.io.min.js'),
      },
      fallback: {
        crypto: 'crypto-js',
        assert: false,
        buffer: false,
        http: false,
        https: false,
      },
    },
    module: {
      rules: [
        {
          test: /\.m?js$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {},
          },
        },
        {
          test: /\.css$/i,
          use: ['style-loader', 'css-loader'],
        },
        {
          test: /\.html$/i,
          loader: 'html-loader',
        },
        {
          test: /\.(svg|ttf)$/i,
          use: [
            {
              loader: 'file-loader',
            },
          ],
        },
      ],
    },
    plugins: [
      new webpack.optimize.LimitChunkCountPlugin({
        maxChunks: 1
      }),
    ],
  }

  if (argv.mode === 'development') {
    config.devtool = 'source-map'
  }

  return config;
}
