const puppeteer = require('puppeteer');
const cas = require('../../cas.js');
const express = require('express');
const assert = require('assert');
const auth = require('basic-auth');

(async () => {
    const authenticate = (req, res, next) => {
        const credentials = auth(req);
        if (!credentials || credentials.name !== 'restapi' || credentials.pass !== 'YdCP05HvuhOH^*Z') {
            res.set('WWW-Authenticate', 'Basic realm="Authentication Required"');
            res.status(401).send('Authentication Required');
        } else {
            console.log("Authenentication successful");
            next();
        }
    };

    let app = express();
    app.post(/^(.+)$/, authenticate, (req, res) => {
        try {
            console.log("Received request");
            const data = {
                "@class": "org.apereo.cas.authentication.principal.SimplePrincipal",
                "id": "casuser",
                "attributes": {
                    "@class": "java.util.LinkedHashMap",
                    "names": [
                        "java.util.List", ["cas", "user"]
                    ]
                }
            };
            res.json(data);
        } catch (e) {
            throw e;
        }
    });
    app.use(authenticate);
    
    let server = app.listen(5432, async () => {
        console.log(`Listening...`);

        const browser = await puppeteer.launch(cas.browserOptions());
        const page = await cas.newPage(browser);
        await cas.goto(page, "https://localhost:8443/cas/login");
        await cas.loginWith(page, "restapi", "YdCP05HvuhOH^*Z");
        await cas.assertCookie(page);

        server.close(() => {
            console.log('Exiting server...');
            browser.close();
        });
        await process.exit(0);
    });

})();
