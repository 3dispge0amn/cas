const puppeteer = require('puppeteer');
const cas = require('../../cas.js');
const assert = require('assert');
const fs = require("fs");

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    const url = `https://localhost:8443/cas/oidc/oidcAuthorize?state=1001&client_id=client&redirect_uri=${encodeURIComponent("https://httpbin.org/anything/client")}&scope=${encodeURIComponent("openid profile")}&response_type=code&nonce=vn4qulthnx`;
    await cas.goto(page, url);

    await cas.loginWith(page, "casuser", "Mellon");
    await page.waitForTimeout(1000);

    await cas.click(page, "#allow");
    await page.waitForNavigation();
    const pageUrl = await page.url();
    console.log(`Page url: ${pageUrl}\n`);
    const response = new URL(pageUrl).hash.replace("#response=", "");
    console.log(`Reading keystore from ${process.env.OIDC_KEYSTORE}`);
    const keyContent = JSON.parse(fs.readFileSync(process.env.OIDC_KEYSTORE, 'utf8'));
    cas.decryptJwtWithJwk(response, keyContent.keys[1], "RS256").then(verified => {
        assert(verified.payload.aud === "client");
        assert(verified.payload.iss === "https://localhost:8443/cas/oidc");
        assert(verified.payload.state === "1001");
        assert(verified.payload.nonce === "vn4qulthnx");
        assert(verified.payload.code !== undefined);
    });
    await browser.close();
})();

