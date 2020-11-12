# PDFIMAGES

The `pdfimages` is a utility program that converts a PDF file into a list of images (each page is converted into an image) and vice versa, a list of image files into a PDF document. It has been useful for me several times to send manually signed documents (get the PDF, in some pages use `gimp` or another program to add the extra data, sign with the pencil tool,... and re-create the PDF again). Apache `pdfbox` is used to manage PDF files.

## Usage

1. Package the program with all the dependencies:

```bash
mvn clean package assembly:single
```

2. The utility has a short usage:

```bash
java -jar pdfimages.jar pdf2images [options] file.pdf [image-prefix]
  Transforms the pdf in a series of images one per page.
  Options:
  --dpi -d dpi: DPIs of the image to produce (default 150)
  --format -f format: format of the image supported by ImageIO.
    * jpg (default)
    * bmp
    * gif
    * png
    * wbmp
    * jpeg
  --type -t type: ImageType to produce.
    * BINARY
    * GRAY
    * RGB (default)
    * ARGB

java -jar pdfimages.jar images2pdf image1 image2 ... file.pdf
  Converts the images in a PDF file, one image per page.
```

3. Convert a PDF file into a list of image files (by default the same file name is used for images without the extension):

```bash
java -jar target/pdfimages-0.1.0-jar-with-dependencies.jar pdf2images file.pdf
```

4. Convert the resulting images back into a PDF document:

```bash
java -jar target/pdfimages-0.1.0-jar-with-dependencies.jar images2pdf file.0.jpg new-file.pdf
```
