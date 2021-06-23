// webpack.config.js
var webpack = require('webpack');
module.exports = {
  entry: {
    entry: __dirname + '/app.js',
  },
  output: {
    filename: '[name].bundle.js',
  },
  resolve: {
    extensions: ['.js'],
    fallback: {
      crypto: 'crypto-js',
    },
  },
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader'],
      },
    ],
  },
};
