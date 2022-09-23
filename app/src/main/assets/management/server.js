require("js/sha256.min.js");
var sqliteDb = 'ShareFileServerDatabase';
var homeClassTable = "share_file_class";
var queryFields = new Array();
var filters = new Array();
var keys = new Array();
var values = new Array();
var sha256key = "sharefileserver1@A)~}:";

function get(queryParams) {
	var filetype;
	if (queryParams != null && queryParams.length > 0) {
		queryParams = JSON.parse(queryParams);
		filtersInit(queryParams);
		filetype = filters[0];
	} else {
		queryFields = new Array();
		filters = new Array();
	}
	var data = app.queryDataBase(sqliteDb, queryFields.length == 0 ? homeClassTable : 'share_file', queryFields, filters);

	data = JSON.parse(data);
	for (var i = 0; i < data.length; i++) {

		try {
			if (filetype != null) {
				filtersInit({
					url: decodeURIComponent(data[i].url)
				});
				var authtable = app.queryDataBase(sqliteDb, "share_file_key", queryFields, filters);
				authtable = JSON.parse(authtable);
				data[i].auth = authtable[0].auth;
			}
		} catch (err) {
			console.log('err:' + err + ',url:' + data[i].thumbnail);
		}

		if (data[i].thumbnail == null || data[i].thumbnail.length == 0)
			continue;
		try {
			filtersInit({
				url: decodeURIComponent(data[i].thumbnail)
			});
			var authtable = app.queryDataBase(sqliteDb, "share_file_key", queryFields, filters);
			authtable = JSON.parse(authtable);
			data[i].thumbauth = authtable[0].auth;
		} catch (err) {
			console.log('err:' + err + ',thumbnail:' + data[i].thumbnail);
		}



	}
	var result = {};
	result.code = Object.keys(data).length > 0 ? 200 : 500;
	result.message = Object.keys(data).length > 0 ? "OK" : "Query Failed";
	if (filetype != null) {
		result.title = getTypeTitle(filetype);
	} else if (result.code == 500 && queryParams.length == 0) {
		insertHome();
		return get('');
	} else {
		result.title = getHomeTitle();
	}

	result.data = data;
	result.button = getDeleteButtonName();
	return JSON.stringify(result);
}

function getHomeTitle() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Share Files Home Page';
		case 'zh':
			return '分享文件首页';
	}
}

function getShareNoteTitle() {
	var result = {};
	switch (app.getLanguage()) {
		case 'en':
			result.data = 'Shared NotePad';
		case 'zh':
			result.data = '共享记事本';
	}
	result.code = 200;
	result.message = 'success';
	return JSON.stringify(result);
}

function getShareNoteDescription() {
	var result = {};
	switch (app.getLanguage()) {
		case 'en':
			result.data = 'LAN text sharing';
		case 'zh':
			result.data = '局域网文本分享';
	}
	result.code = 200;
	result.message = 'success';
	return JSON.stringify(result);
}

function getDeleteButtonName() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Delete Share Files';
		case 'zh':
			return '删除文件的分享';
	}
}

function addfiles() {
	var paths = JSON.parse(app.getShareFiles());
	console.log('paths=' + paths);
	var result = {};
	result.title = getAddFilesTitle();
	result.data = [];
	for (var i = 0; i < paths.length; i++) {
		var filetype = app.getFileType(paths[i]);
		var fileThumbnail = app.getThumbnail(paths[i]);
		var auth = sha256(new Date().getTime() + fileThumbnail + sha256key);

		queryFields = [];
		filters = [];
		thumbnailParam = {
			url: fileThumbnail
		};
		filtersInit(thumbnailParam);
		var authtable = app.queryDataBase(sqliteDb, "share_file_key", queryFields, filters);
		authtable = JSON.parse(authtable);
		if (authtable.length > 0) {
			auth = authtable[0].auth;
		} else {
			keys = ["url", "auth", "lastrequest"];

			values = [fileThumbnail, auth, new Date().getTime() + ""];
			app.insertDataBase(sqliteDb, "share_file_key", keys, values);
		}

		var path = paths[i];
		result.data[i] = {
			url: encodeURIComponent(path),
			thumbnail: encodeURIComponent(fileThumbnail),
			auth: auth,
			filename: app.getFileName(paths[i]),
			createtime: app.getFileCreateTime(paths[i]),
			filesize: app.getFileSize(paths[i]),
			type: filetype
		};
	}
	result.code = paths.length > 0 ? 200 : 500;
	result.message = paths.length > 0 ? "OK" : "Not Share Files";
	result.button = getSaveButtonName();
	result.selectbutton = [getSelectAllName(), getUnSelectAllName()];
	return JSON.stringify(result);
}

function getAddFilesTitle() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Please select the file to be shared';
		case 'zh':
			return '请选择需要分享的文件';
	}
}

function getSaveButtonName() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Save Select Files';
		case 'zh':
			return '保存文件的分享';
	}
}

function getSelectAllName() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Select All';
		case 'zh':
			return '全选';
	}
}

function getUnSelectAllName() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Cancel Select All';
		case 'zh':
			return '取消全选';
	}
}

function getuseguide() {
	var wifi = app.getWifiName();
	if (wifi == '<unknown ssid>') {
		wifi = getUnknownWifiName();
	}
	var data = {
		title: getUseTitle(),
		wifi: getWifiGuide() + wifi,
		ip: getIpGuide() + app.getWebIp() + ":8090",
		scanqrcode:getScanQrcode(),
		address:"http://"+app.getWebIp() + ":8090"
	};
	console.log(data);
	var result = {};
	result.message = "Query Success";
	result.code = 200;
	result.data = data;
	return JSON.stringify(result);
}

function getUnknownWifiName() {

	switch (app.getLanguage()) {
		case 'en':
			return 'Unknown Wifi';
		case 'zh':
			return '无法获取Wifi名称';
	}
}

function getUseTitle() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Invite friends to use a computer, smartphone or tablet to do the following:';
		case 'zh':
			return '邀请朋友使用电脑，智能手机或平板电脑进行以下操作：';
	}
}

function getScanQrcode() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Or Scan below QR code Open Web address:';
		case 'zh':
			return '或者扫描下方二维码打开网址：';
	}
}

function getIpGuide() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Web Browser Input Server IP :';
		case 'zh':
			return '浏览器输入以下地址： ';
	}
}

function getWifiGuide() {
	switch (app.getLanguage()) {
		case 'en':
			return 'Connect WIFI :';
		case 'zh':
			return '连接这个Wifi： ';
	}
}

function savefiles(data) {
	data = JSON.parse(data);
	console.log('data=' + data);

	for (var i = 0; i < data.length; i++) {
		var keys = ["url", "thumbnail", "filename", "createtime", "filesize", "type"];
		var values = [decodeURIComponent(data[i].url), data[i].thumbnail, data[i].filename, data[i].createtime + "", data[i].filesize +
			"", data[
				i].type + ""
		];
		app.insertDataBase(sqliteDb, 'share_file', keys, values);

		keys = ["url", "auth", "lastrequest"];
		values = [decodeURIComponent(data[i].url), sha256(new Date().getTime() + data[i].url + sha256key), new Date().getTime() +
			""
		];
		app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	}
	var result = {};
	result.code = 200;
	result.message = getSaveSuccessToast();
	result.requestToast = true;
	return JSON.stringify(result);
}

function getSaveSuccessToast() {
	switch (app.getLanguage()) {
		case 'en':
			return "Save Files OK!";
		case 'zh':
			return "保存分享的文件成功!";
	}
}

function deletefiles(queryParams) {
	queryParams = JSON.parse(queryParams);
	var success = false;
	for (var i = 0; i < queryParams.length; i++) {
		filtersInit(queryParams[i]);
		success = app.deleteDataBase(sqliteDb, 'share_file', queryFields, filters);
		app.deleteDataBase(sqliteDb, 'share_file_key', queryFields, filters);
	}

	var result = {};
	result.code = success ? 200 : 500;
	result.message = getDeleteSuccessAlert(success);
	return JSON.stringify(result);
}

function getDeleteSuccessAlert(success) {
	switch (app.getLanguage()) {
		case 'en':
			return success ? "Delete Files Success!" : "Delete Files Failed!";
		case 'zh':
			return success ? "删除分享的文件成功!" : "删除分享的文件失败!";
	}
}

function getTypeTitle(type) {
	switch (type) {
		case "0":
			switch (app.getLanguage()) {
				case 'en':
					return "Pictures";
				case 'zh':
					return "图片";
			}
		case "1":
			switch (app.getLanguage()) {
				case 'en':
					return "Videos";
				case 'zh':
					return "视频";
			}
		case "2":
			switch (app.getLanguage()) {
				case 'en':
					return "Documents";
				case 'zh':
					return "文档";
			}
		case "3":
			switch (app.getLanguage()) {
				case 'en':
					return "Audios";
				case 'zh':
					return "音频";
			}
		case "4":
			switch (app.getLanguage()) {
				case 'en':
					return "Applications";
				case 'zh':
					return "应用程序";
			}
		case "5":
			switch (app.getLanguage()) {
				case 'en':
					return "Zips";
				case 'zh':
					return "压缩文件";
			}
		case "6":
			switch (app.getLanguage()) {
				case 'en':
					return "Others";
				case 'zh':
					return "其他";
			}
	}
}

function getsharenote() {
	var result = {};
	result.code = 200;
	result.message = 'Query Success!';
	result.data = app.getShareNote();
	return JSON.stringify(result);
}

function getnote() {
	queryParams = {
		id: '1'
	};
	filtersInit();
	var data = app.queryDataBase(sqliteDb, "share_note_text", queryFields, filters);
	data = JSON.parse(data);

	if (data.length == 0) {
		var keys = ['name', 'password', 'note', 'lastrequest'];
		var values = ["", "", "", new Date().getTime()];
		app.insertDataBase(sqliteDb, "share_note_text", keys, values);
		data[0].note = "";
	}
	var result = {};
	result.code = 200;
	result.message = 'Query Success!';
	result.data = data;
	result.placeholder = getNotePlaceholder();
	result.saveButtonName = getNoteSaveButtonName();
	return JSON.stringify(result);
}

function getNotePlaceholder() {
	switch (app.getLanguage()) {
		case 'en':
			return "Please touch here to start typing text Long press to copy and paste";
		case 'zh':
			return "请点击这里开始输入文本,长按复制粘贴";
	}
}

function getNoteSaveButtonName() {
	switch (app.getLanguage()) {
		case 'en':
			return "Save Note";
		case 'zh':
			return "保存记事本";
	}
}

function postnote(data) {
	queryParams = {
		id: '1'
	};
	filtersInit(queryParams);
	paramsInit({
		note: JSON.parse(data).note
	});
	var success = app.updateDataBase(sqliteDb, "share_note_text", queryFields, filters, keys, values);
	var result = {};
	result.code = success ? 200 : 500;
	result.message = getUpdateNoteSuccessAlert(success);
	return JSON.stringify(result);
}

function getUpdateNoteSuccessAlert(success) {
	switch (app.getLanguage()) {
		case 'en':
			return success ? "Update Note Success!" : "Update Note Failed!";
		case 'zh':
			return success ? "更新记事本成功!" : "更新记事本失败!";
	}
}

function getfile(auth, path) {
	queryFields = [];
	filters = [];
	queryParams = {
		url: path
	};
	filtersInit(queryParams);
	var data = app.queryDataBase(sqliteDb, "share_file_key", queryFields, filters);
	data = JSON.parse(data);
	if (data[0].auth == auth) {
		var lastrequestTime = new Date();
		lastrequestTime.setTime(parseInt(data[0].lastrequest) + (1000 * 60 * 60 * 2)); //2小时，刚好看完电影的长度
		if (lastrequestTime < new Date()) { //上次请求时间比现在大于2小时，刷新auth
			keys = [];
			values = [];
			paramsInit({
				auth: sha256(new Date().getTime() + path + sha256key),
				lastrequest: new Date().getTime() + ""
			});
			var success = app.updateDataBase(sqliteDb, "share_file_key", queryFields, filters, keys, values);
		}


		if (path.startsWith("./"))
			path = path.substring(path.indexOf('/') + 1);
		var result = {};
		result.code = 200;
		result.requestStorageURL = path;
		filtersInit({
			url: path
		});
		data = app.queryDataBase(sqliteDb, "share_file", queryFields, filters);
		data = JSON.parse(data);
		result.filename = Object.keys(data).length > 0 && data[0].filename != null ? data[0].filename : '';
		return JSON.stringify(result);
	} else {
		var result = {};
		result.code = 500;
		result.message = 'auth failed';
		return JSON.stringify(result);
	}

}

function filtersInit(queryParams) {
	queryFields = [];
	filters = [];
	for (var key in queryParams) {
		queryFields[queryFields.length] = key;
		filters[filters.length] = queryParams[key] + "";
	}
}

function paramsInit(appinfo) {
	keys = [];
	values = [];
	for (var key in appinfo) {
		keys[keys.length] = key;
		values[values.length] = appinfo[key] + "";
	}
}

function main() {
	/* 0 图片 1 视频 2 文档 3 音乐 4 应用程序 5 压缩文件 6 其他 */
	app.registerShareFile('addfile.html'); //分享时弹出的窗口内嵌网页
	var dbName = sqliteDb;
	var createHome = "CREATE TABLE IF NOT EXISTS " + homeClassTable + "(" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
		"url TEXT," +
		"thumbnail TEXT," +
		"filename TEXT," +
		"createtime DATE," +
		"filesize INTEGER," +
		"type INTEGER)";
	var createFile = "CREATE TABLE IF NOT EXISTS share_file(" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
		"url TEXT UNIQUE," +
		"thumbnail TEXT," +
		"filename TEXT," +
		"createtime DATE," +
		"filesize INTEGER," +
		"type INTEGER)";
	var createShareFileKey = "CREATE TABLE IF NOT EXISTS share_file_key(" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
		"url TEXT UNIQUE," +
		"auth TEXT," +
		"lastrequest DATE NOT NULL)";
	var createNoteText = "CREATE TABLE IF NOT EXISTS share_note_text(" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
		"name TEXT," +
		"password TEXT," +
		"note TEXT," +
		"lastrequest DATE)";
	var createDbs = [createHome, createFile, createShareFileKey, createNoteText];
	var success = app.createDataBase(dbName, createDbs);
	var result = {};
	result.code = success ? 200 : 500;
	result.message = success ? "OK" : "Create DataBase Failed";

	return JSON.stringify(result);
}

function insertHome() {
	var keys = ["url", "thumbnail", "filename", "createtime", "filesize", "type"];
	var values = ["./index.html?type=2", "./imgs/doc.png", getTypeTitle(2 + ""), new Date().getTime() + "", 0 + "", 2 + ""];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);
	var values = ["./index.html?type=0", "./imgs/pic.png", getTypeTitle(0 + ""), new Date().getTime() + "", 0 + "", 0 + ""];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);
	var values = ["./index.html?type=1", "./imgs/mov.png", getTypeTitle(1 + ""), new Date().getTime() + "", 0 + "", 1 + ""];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);
	var values = ["./index.html?type=3", "./imgs/audio.png", getTypeTitle(3 + ""), new Date().getTime() + "", 0 + "", 3 +
		""
	];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);
	var values = ["./index.html?type=4", "./imgs/app.png", getTypeTitle(4 + ""), new Date().getTime() + "", 0 + "", 4 + ""];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);
	var values = ["./index.html?type=5", "./imgs/zip.png", getTypeTitle(5 + ""), new Date().getTime() + "", 0 + "", 5 + ""];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);
	var values = ["./index.html?type=6", "./imgs/other.png", getTypeTitle(6 + ""), new Date().getTime() + "", 0 + "", 6 +
		""
	];
	app.insertDataBase(sqliteDb, homeClassTable, keys, values);

	var keys = ["url", "auth", "lastrequest"];
	var values = ["./imgs/doc.png", sha256(new Date().getTime() + "./imgs/doc.png" + sha256key), new Date().getTime() + ""];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	var values = ["./imgs/pic.png", sha256(new Date().getTime() + "./imgs/pic.png" + sha256key), new Date().getTime() + ""];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	var values = ["./imgs/mov.png", sha256(new Date().getTime() + "./imgs/mov.png" + sha256key), new Date().getTime() + ""];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	var values = ["./imgs/audio.png", sha256(new Date().getTime() + "./imgs/audio.png" + sha256key), new Date().getTime() +
		""
	];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	var values = ["./imgs/app.png", sha256(new Date().getTime() + "./imgs/app.png" + sha256key), new Date().getTime() + ""];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	var values = ["./imgs/zip.png", sha256(new Date().getTime() + "./imgs/zip.png" + sha256key), new Date().getTime() + ""];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);
	var values = ["./imgs/other.png", sha256(new Date().getTime() + "./imgs/other.png" + sha256key), new Date().getTime() +
		""
	];
	app.insertDataBase(sqliteDb, "share_file_key", keys, values);


}

function exit() {
	app.unregisterShareFile();
}
