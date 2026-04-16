import path from "path";
import { defineConfig } from "tsup";

export default defineConfig([
    {
        entry: [
            'appmap.js',
            'findings.js',
            'install-guide.js',
            'navie.js',
            'signin.js',
            'review.ts',
        ],
        outDir: "dist",
        name: "main",
        noExternal: [/./],
        outExtension: () => ({ js: ".js" }),
        target: "chrome102",
        treeshake: true,
        format: "esm",
        sourcemap: true,
        inject: ["polyfillShim.js"],
        define: {
            global: "globalThis",
        },
        esbuildOptions(options) {
            options.resolveExtensions = [".mjs", ".js", ".ts"];
            options.platform = "browser";
            options.alias = {
                ...(options.alias || {}),
                "process": "process/browser",
                "http": "stream-http",
                "https": "https-browserify",
                "stream": "stream-browserify",

                "socket.io-client": "./node_modules/socket.io-client/dist/socket.io.js",
                messagePublisher: path.resolve("./messagePublisher.js"),
                vsCodeBridge: path.resolve("./vsCodeBridge.js"),
                handleAppMapMessages: path.resolve("./handleAppMapMessages.js"),
            };
        },
        loader: {
            ".html": "text",
        },
    },
]);
