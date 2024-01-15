/* eslint-disable */
const path = require('path');

module.exports = (env, argv) => {
  const isProduction = argv.mode === 'production'

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
        vue: path.resolve('./node_modules/vue'),
        vuex: path.resolve('./node_modules/vuex'),
      },
      fallback: {
        crypto: 'crypto-js',
        assert: false,
        buffer: false,
        http: false,
        https: false,
      }
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
  }

  if (!isProduction) {
    config.devtool = 'source-map'
  }

  return config
}
