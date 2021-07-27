// webpack.config.js
const path = require('path');
const webpack = require('webpack');
module.exports = {
    entry: {
        entry: __dirname + '/app.js',
    },
    output: {
        filename: '[name].bundle.js',
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
};
