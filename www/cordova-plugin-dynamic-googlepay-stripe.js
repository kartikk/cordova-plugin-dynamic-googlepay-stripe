var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'cordova-plugin-dynamic-googlepay-stripe', 'coolMethod', [arg0]);
};
