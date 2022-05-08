'use strict';

var idSymbol = document.querySelector('#symbol');
var idBidQty = document.querySelector('#bidQty');
var idBidPx = document.querySelector('#bidPx');
var idAskQty = document.querySelector('#askQty');
var idAskPx = document.querySelector('#askPx');
var idUtime = document.querySelector('#utime');
var idSpread = document.querySelector('#spread');

var msgStatus = document.querySelector('#msgStatus');

var stompClient = null;
var username = null;

function connect()
{
    var socket = new SockJS('/stomp');

    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);
}


function onConnected()
{
	msgStatus.textContent = 'Connected';
    msgStatus.style.color = 'green';
    
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/ws.register", {}, JSON.stringify({username: 'user_xyz'}) );

}


function onError(error)
{
    msgStatus.textContent = 'Unable to connect. Please refresh this page to try again!';
    msgStatus.style.color = 'red';
}


function onMessageReceived(payload)
{
	console.log("onMessageReceived() " + payload);
	
    var message = JSON.parse(payload.body);

	idSymbol.textContent = message.s;
		
	idBidQty.textContent = message.B;
	idBidPx.textContent = parseFloat(message.b).toFixed(2);
	idAskQty.textContent = message.A;
	idAskPx.textContent = parseFloat(message.a).toFixed(2);

	var spr = message.a - message.b;
	
	idSpread.textContent = parseFloat(spr).toFixed(2);

	//idUtime.textContent = message.u;
    
    //msgStatus.textContent = 'Connected';
    //msgStatus.style.color = 'green';
}


connect();

