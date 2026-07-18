const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');

function parseSemVer(versionStr) {
    if (versionStr.startsWith('v')) {
        versionStr = versionStr.substring(1);
    }
    const [main, prerelease] = versionStr.split('-');
    const parts = main.split('.').map(Number);
    return {
        parts,
        prerelease: prerelease || null
    };
}

function compareSemVer(v1, v2) {
    const s1 = parseSemVer(v1);
    const s2 = parseSemVer(v2);

    for (let i = 0; i < 3; i++) {
        const p1 = s1.parts[i] || 0;
        const p2 = s2.parts[i] || 0;
        if (p1 !== p2) {
            return p1 - p2;
        }
    }

    if (s1.prerelease && !s2.prerelease) return -1;
    if (!s1.prerelease && s2.prerelease) return 1;

    if (s1.prerelease && s2.prerelease) {
        return s1.prerelease.localeCompare(s2.prerelease, undefined, { numeric: true, sensitivity: 'base' });
    }

    return 0;
}

const cacheDir = process.argv[2] || path.join(os.homedir(), '.cache', 'appmap-test');

console.log(`Target cache directory: ${cacheDir}`);

if (!fs.existsSync(cacheDir)) {
    console.log(`Cache directory does not exist. Nothing to clean.`);
    if (process.env.GITHUB_OUTPUT) {
        fs.appendFileSync(process.env.GITHUB_OUTPUT, `cache_hash=empty\n`);
    }
    process.exit(0);
}

try {
    const files = fs.readdirSync(cacheDir);
    // Matches names like appmap-linux-x64-1.2.3 and appmap-win-x64-1.2.3.exe
    // The lazy quantifier "+?" ensures we do not greedily swallow .exe if present
    const nameRegex = /^(appmap|scanner)-([a-z0-9]+)-([a-z0-9]+)-([0-9a-zA-Z_.-]+?)(?:\.exe)?$/i;

    const groups = {};

    for (const file of files) {
        const match = file.match(nameRegex);
        if (!match) {
            console.log(`Skipping non-matching file: ${file}`);
            continue;
        }

        const tool = match[1].toLowerCase();
        const platform = match[2].toLowerCase();
        const arch = match[3].toLowerCase();
        const version = match[4];

        const key = `${tool}-${platform}-${arch}`;
        if (!groups[key]) {
            groups[key] = [];
        }
        groups[key].push({
            filename: file,
            version: version
        });
    }

    for (const key of Object.keys(groups)) {
        const groupFiles = groups[key];
        if (groupFiles.length <= 1) {
            console.log(`Group ${key} has ${groupFiles.length} file(s). No cleanup needed.`);
            continue;
        }

        // Sort descending (highest version first)
        groupFiles.sort((a, b) => compareSemVer(b.version, a.version));

        const keep = groupFiles[0];
        console.log(`Group ${key}: keeping ${keep.filename} (${keep.version})`);

        for (let i = 1; i < groupFiles.length; i++) {
            const discard = groupFiles[i];
            const fullPath = path.join(cacheDir, discard.filename);
            console.log(`  Deleting obsolete version: ${discard.filename} (${discard.version})`);
            try {
                fs.unlinkSync(fullPath);
            } catch (err) {
                console.error(`  Failed to delete ${discard.filename}:`, err);
            }
        }
    }

    // Compute a hash of the remaining matching files to uniquely identify this cache state.
    // Since we delete obsolete files, stub files, etc., the remaining files will be exactly
    // the stable versions we want to cache.
    const remainingFiles = fs.readdirSync(cacheDir)
        .filter(f => nameRegex.test(f))
        .sort();

    const hashInput = remainingFiles.join(',');
    const hash = crypto.createHash('sha256').update(hashInput).digest('hex');

    console.log(`Remaining cache files: ${remainingFiles.join(', ') || '(none)'}`);
    console.log(`Generated cache hash: ${hash}`);

    if (process.env.GITHUB_OUTPUT) {
        fs.appendFileSync(process.env.GITHUB_OUTPUT, `cache_hash=${hash}\n`);
    }

    console.log('Cache cleanup complete.');
} catch (err) {
    console.error('Error during cache cleanup:', err);
    process.exit(1);
}
