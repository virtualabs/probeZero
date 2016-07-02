var async = require('async');
var noble = require('noble');
var fs = require('fs');
var tracker = require('./tracker');
var config = require('./config');

var args = process.argv.slice(2);
var deviceMac = args[0];

var Probe0 = function(){
  this.mode = null;
  this.synced = false;
  this.timeshift = 0;
  this.peripherals = {};
  this.sync_timeout = 60;

  /* Enable scan. */
  noble.on('stateChange', (function(t){
    return function(state) {
        if (state === 'poweredOn') {
            /* Start scanning, allow duplicates. */
            t.setMode('scan');
            noble.startScanning(null, true);
        } else {
            noble.stopScanning();
        }
      };
    })(this));

  /* Launch sync timeout */
  setTimeout((function(t){
    return function(){
      if (!t.synced) {
        console.log('[!] No sync found, start monitoring ...');
        t.synced = true;
        t.setMode('scan');
      }
    };
  })(this), this.sync_timeout*1000);
};

/**
 * Set probe mode
 **/

Probe0.prototype.setMode = function(mode) {
  if (mode == 'scan') {

    if (!this.synced)
      console.log('[i] Waiting for sync ...');

    noble.on('discover', (function(t){
      return function(peripheral){
        var manufacturer = peripheral.advertisement.manufacturerData;
        if ((manufacturer != null) && (manufacturer.slice(2).toString() == "Probe0")) {
          if ((!t.synced) && (t.mode != 'sync')) {
            t.mode = 'sync';
            t.performSync(peripheral);
          }
        }
      };
    })(this));

    /* Track BLE advertisement reports. */
    noble._bindings._gap._hci.on('leAdvertisingReport', (function(t){
      return function(status, type, address, addressType, report, rssi){
        tracker.cleanup();
        if (!tracker.contains(address)) {
          var device = tracker.add(address, report);
          var manufacturer = device.getManufacturerData();
          var is_master = ((manufacturer != null) && (manufacturer.slice(2).toString() == 'Probe0'));

          if (!t.synced) {
            tracker.remove(address);
          } else if (!is_master){
            /* Apply time shift. */
            var timestamp = device.timestamp + t.timeshift;

            if (device.name == null)
              var name = '';
            else
              var name = device.name;

            if (device.model == null)
              var model = '';
            else
              var model = device.model;
            fs.appendFile(
              config.logfile, timestamp + ';'+device.address+';"'+name+'";"'+model+'";'+device.signature + "\n",
              'utf-8'
            );
            console.log(timestamp + ';'+device.address+';"'+name+'";"'+model+'";'+device.signature);
          }
        } else {
          tracker.seen(address);
        }
      };
    })(this));

    noble.startScanning();

  } else if (mode == 'sync') {
  } else {
    /* Stop scanning. */
    noble.stopScanning();
  }
}

Probe0.prototype.performSync = function(peripheral) {
  /* stop scanning. */
  noble.stopScanning();

  /* Connect to master. */
  var me = this;
  peripheral.connect(function(error){
      if (error == null) {
        peripheral.discoverServices(null, function(error, services){
            if (error == null)  {
              for (var i in services) {
                if (services[i].uuid == 'c4453ea53ef14228b4f185ab7827024e') {
                  services[i].discoverCharacteristics(null, function(error, chars){
                      for (var c in chars) {
                        if (chars[c].uuid == '0424587bbbd34eeaa912200cf4c1b317') {
                          /* Read characteristic. */
                          chars[c].read(function(err, data){
                            if (data.length == 4) {
                              var currentTime = data.readUInt32LE(0);
                              var now = Math.floor(new Date() / 1000);
                              me.timeshift = currentTime - now;
                              me.synced = true;
                              console.log('[i] Timeshift: '+ me.timeshift);
                              console.log('[i] Probe synced, start scanning...');
                              me.setMode('scan');
                            } else {
                              console.log('[!] Error while syncing, start anyway.')
                              me.synced = true;
                              me.setMode('scan');
                            }
                          })
                        }
                      }
                  });
                }
              }
            } else {
              console.log('no service');
              this.setMode('scan');
            }
        });
      } else {
        this.setMode('scan');
      }
  });
}

var probe = new Probe0();
