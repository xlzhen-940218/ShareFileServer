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
	//if (data[0].auth == auth) {
		//var lastrequestTime = new Date();
		//lastrequestTime.setTime(parseInt(data[0].lastrequest) + (1000 * 60 * 60 * 2)); //2小时，刚好看完电影的长度
		//if (lastrequestTime < new Date()) { //上次请求时间比现在大于2小时，刷新auth
		//	keys = [];
		//	values = [];
		//	paramsInit({
		//		auth: sha256(new Date().getTime() + path + sha256key),
		//		lastrequest: new Date().getTime() + ""
		//	});
		//	var success = app.updateDataBase(sqliteDb, "share_file_key", queryFields, filters, keys, values);
		//}


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
	//} else {
	//	var result = {};
	//	result.code = 500;
	//	result.message = 'auth failed';
	//	return JSON.stringify(result);
	//}

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

}

function exit() {}
