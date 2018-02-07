export class GeneratedElementDescriptionController {

    constructor() {

    }

    downloadJsonLd() {
        this.openSaveAsDialog(this.element.name + ".jsonld", this.jsonld, "application/json");
    }

    downloadJava() {
        this.openSaveAsDialog(this.element.name + ".java", this.jsonld, "application/java");
    }

    openSaveAsDialog(filename, content, mediaType) {
        var blob = new Blob([content], {type: mediaType});
        saveAs(blob, filename);
    }
}