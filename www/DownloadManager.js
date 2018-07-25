var exec = require('cordova/exec');

exports.download = function(url, destination, mimeType, title, description, success, error) {
    exec(success, error, "DownloadManager", "download", [url, destination, mimeType, title, description]);
};
