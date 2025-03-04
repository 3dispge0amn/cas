const puppeteer = require('puppeteer');
const cas = require('../../cas.js');

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    await cas.goto(page, "https://localhost:8443/cas/login?authn_method=GoogleAuth");
    await cas.loginWith(page);
    await page.waitForTimeout(3000);
    await cas.screenshot(page);
    await cas.assertVisibility(page, '#form-1');
    await cas.assertVisibility(page, '#name-RecordName');
    await cas.assertVisibility(page, '#id-1');
    await cas.assertVisibility(page, '#form-2');
    await cas.assertVisibility(page, '#name-RecordName2');
    await cas.assertVisibility(page, '#id-2');
    await cas.assertVisibility(page, '#register');
    await cas.submitForm(page, "#form-1");
    await cas.assertTextContentStartsWith(page, "#login p", "Your selected device for multifactor authentication is");
    await cas.assertVisibility(page, '#token');
    await cas.assertVisibility(page, '#login');
    await cas.assertVisibility(page, '#cancel');
    await cas.assertVisibility(page, '#register');
    await cas.assertVisibility(page, '#selectDeviceButton');

    await browser.close();
})();
