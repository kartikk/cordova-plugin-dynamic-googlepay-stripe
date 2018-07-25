var exec = require('cordova/exec');

var executeCallback = function(callback, message) {
    if (typeof callback === 'function') {
        callback(message);
    }
};

var GooglePay = {
    initGooglePayClient: function (publishableKey, successCallback, errorCallback) {
        return new Promise(function (resolve, reject) {
            cordova.exec(function(message) {
                executeCallback(successCallback, message);
                resolve(message);
            }, function(message) {
                executeCallback(errorCallback, message);
                reject(message);
            }, 
            'CordovaPluginDynamicGooglepayStripe', 
            'init_google_pay_client', 
            [publishableKey])
        })
    },
    isReadytoPay: function (successCallback, errorCallback) {
        return new Promise(function (resolve, reject) {
            cordova.exec(function(message) {
                executeCallback(successCallback, message);
                resolve(message);
            }, function(message) {
                executeCallback(errorCallback, message);
                reject(message);
            }, 
            'CordovaPluginDynamicGooglepayStripe', 
            'is_ready_to_pay', 
            [])
        })
    },    
    requestPayment: function (totalPrice, currency, successCallback, errorCallback) {
        return new Promise(function (resolve, reject) {
            cordova.exec(function(message) {
                executeCallback(successCallback, message);
                resolve(message);
            }, function(message) {
                executeCallback(errorCallback, message);
                reject(message);
            }, 
            'CordovaPluginDynamicGooglepayStripe', 
            'request_payment', 
            [totalPrice, currency]);
        })
    }
}

module.exports = GooglePay;