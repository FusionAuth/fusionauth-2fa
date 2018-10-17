/*
 * Copyright (c) 2015-2018, FusionAuth, All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

/*
 http://jsfiddle.net/russau/ch8PK/
 */
function dec2hex(s) { return (s < 15.5 ? '0' : '') + Math.round(s).toString(16); }
function hex2dec(s) { return parseInt(s, 16); }

function base32Tohex(base32) {
    var base32chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    var bits = "";
    var hex = "";

    // for each character in string, look up the character in the base32chars
    for (var i = 0; i < base32.length; i++) {

        var val = base32chars.indexOf(base32.charAt(i).toUpperCase());
        bits += leftpad(val.toString(2), 5, '0');
    }

    for (var i = 0; i+4 <= bits.length; i+=4) {
        var chunk = bits.substr(i, 4);
        //console.info("Chunk base2 :" + parseInt(chunk, 2) + " --> " + parseInt(chunk, 2).toString(16));
        hex = hex + parseInt(chunk, 2).toString(16) ;
    }
    return hex;
}

function leftpad(str, len, pad) {
    if (len + 1 >= str.length) {
        str = Array(len + 1 - str.length).join(pad) + str;
    }
    return str;
}

function updateOtp() {

    var key = base32Tohex($('#secret').val());
    var epoch = Math.round(new Date().getTime() / 1000.0);
    var time = leftpad(dec2hex(Math.floor(epoch / 30)), 16, '0');

    // updated for jsSHA v2.0.0 - http://caligatio.github.io/jsSHA/
    var shaObj = new jsSHA("SHA-1", "HEX");
    shaObj.setHMACKey(key, "HEX");
    shaObj.update(time);
    var hmac = shaObj.getHMAC("HEX");

    $('#hmac').empty();

    if (hmac == 'KEY MUST BE IN BYTE INCREMENTS') {
        $('#hmac').append($('<span/>').addClass('label important').append(hmac));
    } else {
        var offset = hex2dec(hmac.substring(hmac.length - 1));
        var part1 = hmac.substr(0, offset * 2);
        var part2 = hmac.substr(offset * 2, 8);
        var part3 = hmac.substr(offset * 2 + 8, hmac.length - offset);
        if (part1.length > 0 ) $('#hmac').append($('<span/>').addClass('label label-default').append(part1));
        $('#hmac').append($('<span/>').addClass('label label-primary').append(part2));
        if (part3.length > 0) $('#hmac').append($('<span/>').addClass('label label-default').append(part3));
    }

    var otp = (hex2dec(hmac.substr(offset * 2, 8)) & hex2dec('7fffffff')) + '';
    otp = (otp).substr(otp.length - 6, 6);

    $('#otp').text(otp);
}

/*
 * http://forthescience.org/blog/2010/11/30/base32-encoding-in-javascript/
 *
 *  Licensed under the - 'WTFPL – Do What the Fuck You Want to Public License'
 *    http://www.wtfpl.net/
 *
 *  Inversoft - Modified somewhat.
 */
var Base32 = Base32 || {};

/**
 *
 * @param s string
 * @returns {string}
 */
Base32.encode = function(s) {
    /* encodes a string s to base32 and returns the encoded string */
    var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    var parts = [];
    var quanta= Math.floor((s.length / 5));
    var leftover = s.length % 5;

    if (leftover != 0) {
        for (var i = 0; i < (5-leftover); i++) { s += '\x00'; }
        quanta += 1;
    }

    for (i = 0; i < quanta; i++) {
        parts.push(alphabet.charAt(s.charCodeAt(i*5) >> 3));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5) & 0x07) << 2)
                                    | (s.charCodeAt(i*5+1) >> 6)));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5+1) & 0x3F) >> 1) ));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5+1) & 0x01) << 4)
                                    | (s.charCodeAt(i*5+2) >> 4)));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5+2) & 0x0F) << 1)
                                    | (s.charCodeAt(i*5+3) >> 7)));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5+3) & 0x7F) >> 2)));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5+3) & 0x03) << 3)
                                    | (s.charCodeAt(i*5+4) >> 5)));
        parts.push(alphabet.charAt( ((s.charCodeAt(i*5+4) & 0x1F) )));
    }

    var replace = 0;
    if (leftover == 1) replace = 6;
    else if (leftover == 2) replace = 4;
    else if (leftover == 3) replace = 3;
    else if (leftover == 4) replace = 1;

    for (i = 0; i < replace; i++) parts.pop();
    // don't pad the encoded value
    // for (i = 0; i < replace; i++) parts.push("=");

    return parts.join("");
}
