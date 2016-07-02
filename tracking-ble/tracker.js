var SIGNATURES = {
  'Fitbit Charge HR': '06:ba5689a6fabfa2bd01467d6e00fbabad,09:436861726765204852,0a:fa,16:0a1812',
  'Fitbit Charge': '06:ba5689a6fabfa2bd01467d6e00fbabad,09:436861726765,0A:fa,16:0a1808',
  'Fitbit One': '09:4f6e65,0A:fa,16:0a1805',
  'Fitbit Flex': '01:06,06:ba5689a6fabfa2bd01467d6ea753abad,09:466c6578,0A:fa,16:0a1807',
  'Fitbit surge': '01:06,06:ba5689a6fabfa2bd01467d6e00fbabad,09:5375726765,16:0a1810',
  'Wistiki': '01:04,09:57697374696b692d,0A:00',
  'Gigaset G-tag': '01:06,02:0f18,09:4769676173657420472d746167,FF:800102151234',
  'Jawbone UP2': '01:06,06:bc4f45f35650a19c1141804500101c15,09:555032,16:0a1802',
  'Jawbone UP3': '01:06,06:bc4f45f35650a19c1141804500101c15,09:555033,16:0a1803',
  'Kensington Eureka': '01:06,02:0a18,09:4b656e73696e67746f6e20457572656b61,FF:a0000201',
  'Apple TV': '01:1a,FF:4c000906',
  'Apple Watch': '01:1a,FF:4c000c0e005c',
  'Gablys lite': '01:06,07:45fa56c1fb1bc02896a867180228174f,09:4741424c5953204c495445,19:0000,FF:abbc',
  'Garmin Forerunner 920': '07:669a0c2000089a94e3117b66103e4e6a,09:466f726572756e6e6572,16:103e000200',
  'Garmin Fenix 3': '07:669a0c2000089a94e3117b66103e4e6a,09:66656e6978203300,16:103e001200',
  'Withings Activité': '01:06,09:57204163746976697465,0A:00,FF:0024e436',
  'Nike+ FuelBand SE': '01:06,07:669a0c200008c181e211dd3110c4cd83',
  'Chipolo': '01:06,06:d54e8938944fd483774f33f8d6851045,09:436869706f6c6f',
  'Wiko Cink Peax 2': '01:02,09:43494e4b20504541582032,09:43494e4b20504541582032',
  'Bose AE2 Soundlink': '01:12,03:befe,09:426f73652041453220536f756e644c696e6b,FF:0033400a',
  'GABLYS LITE': '01:06,07:45fa56c1fb1bc02896a867180228174f,09:4741424c5953204c495445,19:0000',
  'vivosmart/vivoactive': '01:06,07:669a0c2000089a94e3117b66103e4e6a,16:103e001200',
}

function sortNumber(a,b) {
    return a - b;
}

function hex(v) {
  var hex_ = v.toString(16);
  if (hex_.length < 2)
    return '0'+hex_;
  else {
    return hex_;
  }
}

/**
 * probe0 device class
 **/

var Device = function(address, adRecords){
  this.records = {};
  this.signature = null;
  this.name = null;
  this.model = null;
  this.address = address;

  /* Keep the date and time we registered this device. */
  this.timestamp = Math.floor(new Date() / 1000);

  /* Parse AD records. */
  this.parseRecords(adRecords);

  /* Identify device. */
  this.identify();
};

Device.prototype.parseRecords = function(records){
  var size = records.length;
  var i = 0;
  var type=0;
  var length=0;
  var record_types = [];

  while(i < (size-1)) {
    length = records.readUInt8(i);
    type = records.readUInt8(i+1);
    var data = records.slice(i+2, i+length+1);
    /* Save complete name if provided. */
    if (((type == 0x09) || (type == 0x08)) && (this.name == null))
      this.name = data.toString();

    /* Save raw record. */
    this.records[type] = data;
    record_types.push(type);
    i += length + 1;
  }

  /* Compute signature: sort records and generate string. */
  signature = [];
  record_types = record_types.sort(sortNumber);
  for (var i in record_types) {
    type = record_types[i];
    signature.push(hex(type)+':'+this.records[type].toString('hex'));
  }
  this.signature = signature.join(',');
};

Device.prototype.is = function(signature) {
  if ((this.signature == null) || (this.signature == ''))
    return false

  /* Split signature. */
  other_records = {};
  var records = signature.split(',');
  for (var record in records) {
    record_info = records[record].split(':');
    type = Buffer(record_info[0], 'hex').readUInt8(0);
    value = Buffer(record_info[1], 'hex');
    other_records[type] = value;
  }
  /* Check records. */
  for (type in this.records) {
    if (type in other_records) {
      if (other_records[type].compare(this.records[type].slice(0, other_records[type].length)) != 0) {
        return false;
      }
    } else {
      return false;
    }
  }

  return true;
}

Device.prototype.identify = function() {
  for (var model in SIGNATURES) {
    if (this.is(SIGNATURES[model]))
      return model;
  }
  return null;
};

Device.prototype.getManufacturerData = function(){
  for (var record in this.records) {
    if (record == 0xFF) {
      return this.records[record];
    }
  }
  return  null;
};

/**
 * DeviceTracker
 *
 * This class is used to track BLE devices from the time we found them
 * until the time we lost them.
 **/

var DeviceTracker = function (){
  this.devices = {};

  /* Timeout in seconds. */
  this.timeout = 60.0;
};

DeviceTracker.prototype.add = function(address, aRecords) {
  if (!(address in this.devices)) {
    this.devices[address] = new Device(address, aRecords);
  }
  return this.devices[address];
};

DeviceTracker.prototype.contains = function(address) {
  return (address in this.devices);
}


/**
 * get()
 *
 * Get device from tracked devices.
 **/

DeviceTracker.prototype.get = function(address) {
  if (address in this.devices)
    return this.devices[address];
  else
    return null;
};

/**
 * remove()
 *
 * Remove a device from the tracked ones.
 **/

DeviceTracker.prototype.remove = function(address) {
  if (address in this.devices) {
    delete this.devices[address];
  }
};

/**
 * seen()
 *
 * mark a device as seen.
 **/

DeviceTracker.prototype.seen = function(address) {
  if (address in this.devices) {
    this.devices[address].timestamp = Math.floor(new Date() / 1000);
  }
}

/**
 * cleanup()
 *
 * Remove devices we'd not seen since a given time.
 **/

DeviceTracker.prototype.cleanup = function() {
  var now = Math.floor(new Date() / 1000);
  for (var address in this.devices) {
    if ((now - this.devices[address].timestamp) > this.timeout) {
      this.remove(address);
    }
  }
}

module.exports = new DeviceTracker();
