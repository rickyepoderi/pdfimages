/*
 * Copyright (c) 2017 ricky <https://github.com/rickyepoderi/pdfimages>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.rickyepoderi.pdfimages;

import java.awt.image.BufferedImage;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.awt.Dimension;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 *
 * <p>A simple class that converts images into a pdf file and vice-versa. The class
 * uses pdfbox and ImageIO to manage pdfs and images respectively. Only two
 * methods are provided:</p>
 * <ul>
 * <li><strong>images2Pdf</strong>: it receives an array of images and creates
 * a pdf with one image in each page.</li>
 * <li><strong>pdf2Images</strong>: it receives the pdf file and creates one
 * image per page.</li>
 * </ul>
 * 
 * <p> This class is based in two previous ideas (but it is significantly 
 * modified):</p>
 * <ul>
 * <li><a href="https://pavanandhukuri.wordpress.com/2014/04/06/convert-images-to-a-single-pdf-using-apache-pdfbox/">
 * Convert Images to a single PDF using Apache PDFBox</a></li>
 * <li><a href="http://www.paulzepernick.com/java/java-apache-pdfbox-convert-multipage-tiff-to-pdf/">
 * Java Apache PDFBox Convert Multipage Tiff To PDF</a></li>
 * </ul>
 */
public class Converter {
    
    public static final String DEFAULT_FORMAT = "jpg";
    public static final ImageType DEFAULT_TYPE = ImageType.RGB;

    /**
     * Empty constructor
     */
    public Converter() {
        // NO-OP
    }

    /**
     * Creates a dimension to scale the image to the size of the page.
     * @param imgSize The image dimension
     * @param boundary The page dimension
     * @return The new dimension of the image after scaling it to the page
     */
    private Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
        int new_width = imgSize.width;
        int new_height = imgSize.height;
        // scale image in width
        if (new_width > boundary.width) {
            new_width = boundary.width;
            new_height = (new_width * imgSize.height) / imgSize.width;
        }
        // scale image in height
        if (new_height > boundary.height) {
            new_height = boundary.height;
            new_width = (new_height * imgSize.width) / imgSize.height;
        }
        return new Dimension(new_width, new_height);
    }
    
    /**
     * Method that converts the images provides to pdf and writes to the target 
     * path specified.
     *
     * @param output The file to write
     * @param files The image files to put in the pdf
     * @throws IOException Some error generating the PDF
     */
    public void images2Pdf(File output, File... files) throws IOException {
        try (PDDocument pdDocument = new PDDocument()) {
            for (File file : files) {
                PDPage page = new PDPage();
                pdDocument.addPage(page);
                try (PDPageContentStream pageStream = new PDPageContentStream(pdDocument, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    BufferedImage bimage = ImageIO.read(file);
                    PDImageXObject img = LosslessFactory.createFromImage(pdDocument, bimage);
                    Dimension scaledDim = getScaledDimension(new Dimension(img.getWidth(), img.getHeight()),
                            new Dimension((int) page.getMediaBox().getWidth(), (int) page.getMediaBox().getHeight()));
                    pageStream.drawImage(img, 0, 0, scaledDim.width, scaledDim.height);
                }
            }
            pdDocument.save(output);
        }
    }
    
    /**
     * Method that converts a PDF file in a series of images. 
     * 
     * @param pdfFile The PDF file to read
     * @param prefix The prefix of the images to write
     * @param suffix The image suffix used for image files ("jpg", "png",...)
     * @param dpi The DPI of the images to render pages
     * @param type The type of the image (RGB, GREY,...)
     * @throws IOException Some error generating the images
     */
    public void pdf2Images(File pdfFile, String prefix, String suffix, int dpi, ImageType type) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int pad = (int) Math.ceil(Math.log10(document.getNumberOfPages()));
        if (pad == 0) {
            pad = 1;
        }
        String format = String.format("%s.%%0%dd.%s", prefix, pad, suffix);
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(i, dpi, type);
            ImageIO.write(image, suffix, new File(String.format(format, i)));
        }
    }
    
    /**
     * Prints usage and throws an IllegalArgumentException.
     * @param error The error message to show.
     */
    public void usage(String error) {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder()
                .append(nl).append(nl)
                .append("Error: ").append(error).append(nl).append(nl)
                .append("USAGE:").append(nl).append(nl);
        // pdf2images
        sb.append("java -jar pdfimages.jar pdf2images [options] file.pdf [image-prefix]").append(nl)
                .append("  Transforms the pdf in a series of images one per page.").append(nl)
                .append("  Options:").append(nl)
                .append("  --dpi -d dpi: DPIs of the image to produce (default 150)").append(nl)
                .append("  --format -f format: format of the image supported by ImageIO.").append(nl);
        for (String format: ImageIO.getWriterFileSuffixes()) {
            sb.append("    * ").append(format).append(DEFAULT_FORMAT.equals(format)? " (default)" : "").append(nl);
        }
        sb.append("  --type -t type: ImageType to produce.").append(nl);
        for (ImageType type: ImageType.values()) {
            sb.append("    * ").append(type).append(type == DEFAULT_TYPE? " (default)" : ""). append(nl);
        }
        // images2pdf
        sb.append(nl)
                .append("java -jar pdfimages.jar images2pdf image1 image2 ... file.pdf").append(nl)
                .append("  Converts the images in a PDF file, one image per page.").append(nl);
        throw new IllegalArgumentException(sb.toString());
    } 
    
    /**
     * Parses the args for images2pdf and call the method.
     * @param args The args without the images2pdf operation
     * @throws IOException Some error
     */
    public void parseImages2Pdf(String[] args) throws IOException {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < args.length - 1; i++) {
            File f = new File(args[i]);
            if (!f.canRead()) {
                usage(String.format("File \"%s\" is not readable.", args[i]));
            }
            String mime = Files.probeContentType(f.toPath());
            Iterator<ImageReader> iter = ImageIO.getImageReadersByMIMEType(mime);
            if (iter == null || !iter.hasNext()) {
                usage(String.format("Cannot find image reader for file \"%s\".", args[i]));
            }
            files.add(f);
        }
        if (files.isEmpty()) {
            usage("You should provide one or more images to add to the pdf file.");
        }
        File output = new File(args[args.length - 1]);
        if (output.canWrite()) {
            usage(String.format("File \"%s\" already exists or is not writable.", args[args.length - 1]));
        }
        images2Pdf(output, files.toArray(new File[0]));
    }
    
    /**
     * Parses an integer from the option.
     * @param args The args used to call the java
     * @param i The current option 
     * @return The integer value
     */
    public int parseInteger(String[] args, int i) {
        if (args.length <= i+1) {
            usage(String.format("The option \"%s\" needs an integer value", args[i]));
        }
        try {
            return Integer.parseInt(args[i+1]);
        } catch(NumberFormatException e) {
            usage(String.format("Invalid integer format for option \"%s\": \"%s\".", args[i], args[i+1]));
            return 0;
        }
    }
    
    /**
     * Parses the format option checking if there is a writer for that suffix.
     * @param args The args used to call the java 
     * @param i The current option
     * @return The format validated by ImageIO
     */
    public String parseFormat(String[] args, int i) {
        if (args.length <= i+1) {
            usage(String.format("The option \"%s\" needs a integer value", args[i]));
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(args[i+1]);
        if (writers == null || !writers.hasNext()) {
            usage(String.format("There is no writer defined for the format \"%s\".", args[i+1]));
        }
        return args[i+1];
    }
    
    /**
     * Parses the type of teh image to produce.
     * @param args The args used to call the java
     * @param i The current option
     * @return The format validated by ImageIO
     */
    public ImageType parseType(String[] args, int i) {
        if (args.length <= i+1) {
            usage(String.format("The option \"%s\" needs a integer value", args[i]));
        }
        try {
            return ImageType.valueOf(args[i+1]);
        } catch(IllegalArgumentException e) {
            usage(String.format("Invalid ImageType for option \"%s\": \"%s\".", args[i], args[i+1]));
            return null;
        }
    }
    
    /**
     * Method that returns the filename without the porssible extension suffix.
     * @param name The loca filename (should not have any slash)
     * @return The filename without the extensions
     */
    public String getFilenameWithoutExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            return name;
        } else {
            return name.substring(0, idx);
        }
    }
    
    /**
     * Parses the args for pdf2images and call the method.
     * @param args The args without the images2pdf operation
     * @throws IOException Some error
     */
    public void parsePdf2Images(String[] args) throws IOException {
        int dpi = 150;
        String format = DEFAULT_FORMAT;
        boolean options = true;
        ImageType type = DEFAULT_TYPE;
        int i;
        for (i = 0; i < args.length && options; i++) {
            switch(args[i]) {
                case "--dpi":
                case "-d":
                    dpi = parseInteger(args, i++);
                    break;
                case "--format":
                case "-f":
                    format = parseFormat(args, i++);
                    break;
                case "--type":
                case "-t":
                    type = parseType(args, i++);
                    break;
                default:
                    options = false;
                    i--;
                    break;
            }
        }
        // check pdf file
        if (args.length <= i) {
            usage("You should pass a PDF file to operatioj pdf2images.");
        }
        File pdf = new File(args[i]);
        if (!pdf.canRead()) {
            usage(String.format("File \"%s\" is not readable.", args[i]));
        }
        String mime = Files.probeContentType(pdf.toPath());
        if (!mime.equals("application/pdf")) {
            usage(String.format("File \"%s\" is not a PDF file.", args[i]));
        }
        // prefix for the images
        String prefix = getFilenameWithoutExtension(pdf.getName());
        i++;
        if (args.length > i) {
            prefix = args[i];
        }
        // more is not allowed
        i++;
        if (args.length > i) {
            usage("Too many arguments.");
        }
        pdf2Images(pdf, prefix, format, dpi, type);
    }
    
    /**
     * Executes the process to convert images into pdf or pdf into images.
     * @param args The args for the command.
     * @throws IOException Some error
     */
    public void execute(String[] args) throws IOException {
        if (args.length < 1) {
            usage("Invalid arguments.");
        } else if ("pdf2images".equals(args[0])) {
            parsePdf2Images(Arrays.copyOfRange(args, 1, args.length));
        } else if ("images2pdf".equals(args[0])) {
            parseImages2Pdf(Arrays.copyOfRange(args, 1, args.length));
        } else {
            usage("Invalid operation. It should be \"files2pdf\" or \"pdf2files\".");
        }
    }
    
    /**
     * Main method. Just create an object of this class and calls the execute
     * method.
     * @param args The args to call the program
     * @throws IOException Some error
     */
    public static void main(String[] args) throws IOException {
        Converter converter = new Converter();
        converter.execute(args);
    }

}
