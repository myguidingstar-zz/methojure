function SockJS(url, dep_protocols_whitelist, options) {};

SockJS.prototype.onopen;
SockJS.prototype.onclose;
SockJS.prototype.onmessage;
SockJS.prototype.onerror;

SockJS.prototype.send = function(message);
SockJS.prototype.close = function(code, reason);

SockJS.prototype.addEventListener = function(eventType, listener);
SockJS.prototype.removeEventListener = function(eventType, listener);
SockJS.prototype.dispatchEvent = function(event);

