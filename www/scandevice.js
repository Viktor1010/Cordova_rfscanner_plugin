/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var controller = null;
var isStart = false;
var timer = null;
var app = {
    // Application Constructor
    initialize: function() {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicitly call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('deviceready');
    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        
        controller = new Controller();
       
    }
};

app.initialize();

var Controller = function() {
    var controller = {
        self: null,
        initialize: function() {
            self = this;
            this.bindEvents();            
        },

        bindEvents: function() {
            console.log("Listener started");
            $(".stop_btn").attr("disabled",true);
        	$(".start_btn").on('click',this.onStart);
        	$(".stop_btn").on('click',this.onStop);
        },
        onStart: function() {
            var interval = parseInt($('#interval').val())*1000;
            if(interval<60000 && interval>0)
               {
               var server=$("#server").val();
                cordova.exec(success, failure, 'ScanDevice', 'start', [interval,server]);
               $('.status.listening').css("display","none");
               $('.status.received').css("display","block");
               $(".stop_btn").attr("disabled",false);
               $(".start_btn").attr("disabled",true);
               }
            else
                showAlert("You have to set interval between 0~60.");
        },

        onStop: function() {
            cordova.exec(success, failure, 'ScanDevice', 'stop', []);
            $('.status.listening').css("display","block");
            $('.status.received').css("display","none");
            $(".stop_btn").attr("disabled",true);
            $(".start_btn").attr("disabled",false);

        }
    }
    controller.initialize();
    return controller;
}

function success(message){
     showAlert(message);
}
function failure(){
     showAlert("Action was failured.");
}
function showAlert(message) {
        navigator.notification.alert(
            message,  // message
            alertDismissed,         // callback
            'Scan Device',            // title
            'Done'                  // buttonName
        );
    }
function alertDismissed(){
    
}