## FusionAuth Two Factor ![semver 2.0.0 compliant](http://img.shields.io/badge/semver-2.0.0-brightgreen.svg?style=flat-square)

This project is a helper for anyone building their own OTP implementation. This project contains a web based tester, and a Java class with some helpful methods.

### Using the Java Helper
The Java helper can be found in `src/main/java/io/fusionauth/twofactor/TwoFactor.java`.

### Web based testing
To use the web based OTP generator with QRCode options, open the `web/html/example.html` file in your browser.

The following is an example screenshot. This tool can be useful for testing.

![Example Screenshot](/web/images/example_screenshot.png)

### Building in Savant
**Note:** This project uses the Savant build tool. To compile using using Savant, follow these instructions:

```bash
$ mkdir ~/savant
$ cd ~/savant
$ wget http://savant.inversoft.org/org/savantbuild/savant-core/1.0.0/savant-1.0.0.tar.gz
$ tar xvfz savant-1.0.0.tar.gz
$ ln -s ./savant-1.0.0 current
$ export PATH=$PATH:~/savant/current/bin/
```

Then, perform an integration build of the project by running:
```bash
$ sb int
```

For more information, checkout [savantbuild.org](http://savantbuild.org/).
