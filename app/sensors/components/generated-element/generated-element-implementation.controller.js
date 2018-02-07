export class GeneratedElementImplementationController {

    constructor() {
        this.extractedFiles = [];
        this.currentFileName = "";
        this.currentFileContents = "";
        this.loadingCompleted = false;

        this.new_zip = new JSZip();

        this.new_zip.loadAsync(this.zipFile)
            .then(function (zip) {

                angular.forEach(zip.files, file => {
                    var filename = file.name;
                    this.extractedFiles.push({
                        "fileNameLabel": this.getFileName(filename),
                        "fileNameDescription": this.getDirectory(filename),
                        "fileName": filename,
                        "fileContents": file
                    });
                })
            });
    }

    openFile(file) {
        this.loadingCompleted = false;
        this.currentFileName = file.fileName;
        file.fileContents.async("string")
            .then(content => {
                this.currentFileContents = content;
                this.loadingCompleted = true;
            });
        ;
    }

    getLanguage(filename) {
        if (filename.endsWith("java")) return "java";
        else if (filename.endsWith("xml")) return "xml";
        else return "";
    }

    getFileName(filename) {
        if (/.+\\/gi.test(filename))
            return filename.replace(/.+\\/g, "");
        else if (/.+\//gi.test(filename))
            return filename.replace(/.+\//g, "");
        else
            return filename;
    }

    getDirectory(filename) {
        if (/.+\\/gi.test(filename)) {
            var directory = /.+\\/gi.exec(filename)[0];
            return directory.replace(/\\/g, "/");
        }
        else if (/.+\//gi.test(filename)) {
            var directory = /.+\//gi.exec(filename)[0];
            return directory.replace(/\//g, "/");
        }
        else return "/";
    }

    downloadZip() {
        this.openSaveAsDialog(this.element.name + ".zip", this.zipFile, "application/zip");
    }

    openSaveAsDialog(filename, content, mediaType) {
        var blob = new Blob([content], {type: mediaType});
        this.saveAs(blob, filename);
    }

}