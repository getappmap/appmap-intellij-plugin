import path from "path";
import { defineConfig } from "tsup";
import polyfills from "node-libs-browser";

const isProduction = process.env.NODE_ENV === "production";

export default defineConfig([
    {
        entry: ["main.js"],
        outDir: "dist",
        name: "main",
        noExternal: [/./],
        outExtension: () => ({ js: ".js" }),
        target: "chrome102",
        minify: isProduction,
        format: "iife",
        sourcemap: true,
        inject: ["polyfillShim.js"],
        define: {
            global: "globalThis",
            process: "process",
            Buffer: "Buffer",
        },
        esbuildOptions(options) {
            options.resolveExtensions = [".mjs", ".js", ".ts"];
            options.mainFields = ["browser", "main"];
            options.alias = {
                ...(options.alias || {}),
                ...Object.entries(polyfills)
                    .filter(([, modulePath]) => Boolean(modulePath))
                    .reduce((memo, [name, modulePath]) => {
                        memo[name] = modulePath;
                        return memo;
                    }, {}),
                fs: "./node_modules/browserify-fs",
                "socket.io-client": "./node_modules/socket.io-client/dist/socket.io.js",
                messagePublisher: path.resolve("../appland-shared/messagePublisher.js"),
                vsCodeBridge: path.resolve("../appland-shared/vsCodeBridge.js"),
                handleAppMapMessages: path.resolve("../appland-shared/handleAppMapMessages.js"),
            };
        },
        loader: {
            ".html": "text",
        },
    },
]);
