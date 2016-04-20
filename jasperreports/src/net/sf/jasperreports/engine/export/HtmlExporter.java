/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2014 TIBCO Software Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.engine.export;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.awt.geom.Dimension2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.tools.codec.Base64Encoder;

import net.sf.jasperreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.jasperreports.crosstabs.interactive.CrosstabInteractiveJsonHandler;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRAnchor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGenericElementType;
import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.JRPen;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintElementIndex;
import net.sf.jasperreports.engine.JRPrintEllipse;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintGraphicElement;
import net.sf.jasperreports.engine.JRPrintHyperlink;
import net.sf.jasperreports.engine.JRPrintHyperlinkParameter;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRPrintImageArea;
import net.sf.jasperreports.engine.JRPrintImageAreaHyperlink;
import net.sf.jasperreports.engine.JRPrintLine;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintRectangle;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.PrintElementId;
import net.sf.jasperreports.engine.PrintElementVisitor;
import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.ReportContext;
import net.sf.jasperreports.engine.export.tabulator.Cell;
import net.sf.jasperreports.engine.export.tabulator.CellVisitor;
import net.sf.jasperreports.engine.export.tabulator.Column;
import net.sf.jasperreports.engine.export.tabulator.ElementCell;
import net.sf.jasperreports.engine.export.tabulator.FrameCell;
import net.sf.jasperreports.engine.export.tabulator.LayeredCell;
import net.sf.jasperreports.engine.export.tabulator.Row;
import net.sf.jasperreports.engine.export.tabulator.SplitCell;
import net.sf.jasperreports.engine.export.tabulator.Table;
import net.sf.jasperreports.engine.export.tabulator.TableCell;
import net.sf.jasperreports.engine.export.tabulator.TablePosition;
import net.sf.jasperreports.engine.export.tabulator.Tabulator;
import net.sf.jasperreports.engine.type.HorizontalImageAlignEnum;
import net.sf.jasperreports.engine.type.HyperlinkTypeEnum;
import net.sf.jasperreports.engine.type.LineDirectionEnum;
import net.sf.jasperreports.engine.type.LineSpacingEnum;
import net.sf.jasperreports.engine.type.LineStyleEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.RotationEnum;
import net.sf.jasperreports.engine.type.RunDirectionEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.VerticalImageAlignEnum;
import net.sf.jasperreports.engine.util.HyperlinkData;
import net.sf.jasperreports.engine.util.ImageUtil;
import net.sf.jasperreports.engine.util.JRCloneUtils;
import net.sf.jasperreports.engine.util.JRColorUtil;
import net.sf.jasperreports.engine.util.JRStringUtil;
import net.sf.jasperreports.engine.util.JRStyledText;
import net.sf.jasperreports.engine.util.JRTextAttribute;
import net.sf.jasperreports.engine.util.JRTypeSniffer;
import net.sf.jasperreports.engine.util.Pair;
import net.sf.jasperreports.export.ExportInterruptedException;
import net.sf.jasperreports.export.ExporterInputItem;
import net.sf.jasperreports.export.HtmlExporterConfiguration;
import net.sf.jasperreports.export.HtmlReportConfiguration;
import net.sf.jasperreports.renderers.AreaHyperlinksRenderable;
import net.sf.jasperreports.renderers.DataRenderable;
import net.sf.jasperreports.renderers.DimensionRenderable;
import net.sf.jasperreports.renderers.Renderable;
import net.sf.jasperreports.renderers.RenderersCache;
import net.sf.jasperreports.renderers.ResourceRenderer;
import net.sf.jasperreports.renderers.util.RendererUtil;
import net.sf.jasperreports.renderers.util.SvgFontProcessor;
import net.sf.jasperreports.search.HitTermInfo;
import net.sf.jasperreports.search.SpansInfo;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class HtmlExporter extends AbstractHtmlExporter<HtmlReportConfiguration, HtmlExporterConfiguration>
{
	private static final Log log = LogFactory.getLog(HtmlExporter.class);
	
	private static final String EXCEPTION_MESSAGE_KEY_INTERNAL_ERROR = "export.html.internal.error";
	private static final String EXCEPTION_MESSAGE_KEY_UNEXPECTED_ROTATION_VALUE = "export.html.unexpected.rotation.value";
	
	/**
	 * The exporter key, as used in
	 * {@link GenericElementHandlerEnviroment#getElementHandler(JRGenericElementType, String)}.
	 */
	public static final String HTML_EXPORTER_KEY = JRPropertiesUtil.PROPERTY_PREFIX + "html";
	
	/**
	 *
	 */
	public static final String HTML_EXPORTER_PROPERTIES_PREFIX = JRPropertiesUtil.PROPERTY_PREFIX + "export.html.";

	/**
	 * @deprecated Replaced by {@link HtmlReportConfiguration#PROPERTY_IGNORE_HYPERLINK}.
	 */
	public static final String PROPERTY_IGNORE_HYPERLINK = HtmlReportConfiguration.PROPERTY_IGNORE_HYPERLINK;

	/**
	 * Property that provides the value for the <code>class</code> CSS style property to be applied 
	 * to elements in the table generated for the report. The value of this property 
	 * will be used as the value for the <code>class</code> attribute of the <code>&lt;td&gt;</code> tag for the element when exported to HTML and/or 
	 * the <code>class</code> attribute of the <code>&lt;span&gt;</code> or <code>&lt;div&gt;</code> tag for the element, when exported to XHTML/CSS.
	 */
	public static final String PROPERTY_HTML_CLASS = HTML_EXPORTER_PROPERTIES_PREFIX + "class";

	/**
	 *
	 */
	public static final String PROPERTY_HTML_ID = HTML_EXPORTER_PROPERTIES_PREFIX + "id";

	/**
	 * @deprecated Replaced by {@link HtmlReportConfiguration#PROPERTY_BORDER_COLLAPSE}.
	 */
	public static final String PROPERTY_BORDER_COLLAPSE = HtmlReportConfiguration.PROPERTY_BORDER_COLLAPSE;

	protected JRHyperlinkTargetProducerFactory targetProducerFactory;		
	
	protected Map<String,String> rendererToImagePathMap;
	protected Map<Pair<String, Rectangle>,String> imageMaps;
	protected RenderersCache renderersCache;

	protected Writer writer;
	protected int reportIndex;
	protected int pageIndex;
	
	protected LinkedList<Color> backcolorStack = new LinkedList<Color>();
	
	protected ExporterFilter tableFilter;
	
	protected int pointerEventsNoneStack = 0;

	private List<HyperlinkData> hyperlinksData = new ArrayList<HyperlinkData>();
	
	public HtmlExporter()
	{
		this(DefaultJasperReportsContext.getInstance());
	}

	public HtmlExporter(JasperReportsContext jasperReportsContext)
	{
		super(jasperReportsContext);

		exporterContext = new ExporterContext();
	}
	
	@Override
	public String getExporterKey()
	{
		return HTML_EXPORTER_KEY;
	}

	@Override
	public String getExporterPropertiesPrefix()
	{
		return HTML_EXPORTER_PROPERTIES_PREFIX;
	}

	@Override
	public void exportReport() throws JRException
	{
		/*   */
		ensureJasperReportsContext();
		ensureInput();

		rendererToImagePathMap = new HashMap<String,String>();
		imageMaps = new HashMap<Pair<String, Rectangle>,String>();
		renderersCache = new RenderersCache(getJasperReportsContext());

		fontsToProcess = new HashMap<String, HtmlFontFamily>();
		
		//FIXMENOW check all exporter properties that are supposed to work at report level
		
		initExport();
		
		ensureOutput();

		writer = getExporterOutput().getWriter();

		try
		{
			exportReportToWriter();
		}
		catch (IOException e)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_OUTPUT_WRITER_ERROR,
					new Object[]{jasperPrint.getName()}, 
					e);
		}
		finally
		{
			getExporterOutput().close();
			resetExportContext();
		}
	}

	
	@Override
	protected Class<HtmlExporterConfiguration> getConfigurationInterface()
	{
		return HtmlExporterConfiguration.class;
	}

	
	@Override
	protected Class<HtmlReportConfiguration> getItemConfigurationInterface()
	{
		return HtmlReportConfiguration.class;
	}


	@Override
	@SuppressWarnings("deprecation")
	protected void ensureOutput()
	{
		if (exporterOutput == null)
		{
			exporterOutput = 
				new net.sf.jasperreports.export.parameters.ParametersHtmlExporterOutput(
					getJasperReportsContext(),
					getParameters(),
					getCurrentJasperPrint()
					);
		}
	}


	@Override
	protected void initExport()
	{
		super.initExport();
	}


	@Override
	protected void initReport()
	{
		super.initReport();

		HtmlReportConfiguration configuration = getCurrentItemConfiguration();
		
		if (configuration.isRemoveEmptySpaceBetweenRows())
		{
			log.info("Removing empty space between rows not supported");
		}

		// this is the filter used to create the table, taking in consideration unhandled generic elements
		tableFilter = new GenericElementsFilterDecorator(jasperReportsContext, HTML_EXPORTER_KEY, filter);
	}
	

	@Override
	protected void setJasperReportsContext(JasperReportsContext jasperReportsContext)
	{
		super.setJasperReportsContext(jasperReportsContext);
		
		targetProducerFactory = new DefaultHyperlinkTargetProducerFactory(jasperReportsContext);
	}

	
	protected void exportReportToWriter() throws JRException, IOException
	{
		HtmlExporterConfiguration configuration = getCurrentConfiguration(); 
		String htmlHeader = configuration.getHtmlHeader();
		String betweenPagesHtml = configuration.getBetweenPagesHtml();
		String htmlFooter = configuration.getHtmlFooter();
		boolean flushOutput = configuration.isFlushOutput();//FIXMEEXPORT maybe move flush flag to output

		if (htmlHeader == null)
		{
			String encoding = getExporterOutput().getEncoding();

			writer.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
			writer.write("<html>\n");
			writer.write("<head>\n");
			writer.write("  <title></title>\n");
			writer.write("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + encoding + "\"/>\n");
			writer.write("  <style type=\"text/css\">\n");
			writer.write("    a {text-decoration: none}\n");
			writer.write("  </style>\n");
			writer.write("</head>\n");
			writer.write("<body text=\"#000000\" link=\"#000000\" alink=\"#000000\" vlink=\"#000000\">\n");
			writer.write("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n");
			writer.write("<tr><td width=\"50%\">&nbsp;</td><td align=\"center\">\n");
			writer.write("\n");
		}
		else
		{
			writer.write(htmlHeader);
		}

		List<ExporterInputItem> items = exporterInput.getItems();
		
		for(reportIndex = 0; reportIndex < items.size(); reportIndex++)
		{
			ExporterInputItem item = items.get(reportIndex);

			setCurrentExporterInputItem(item);
			
			List<JRPrintPage> pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0)
			{
				PageRange pageRange = getPageRange();
				int startPageIndex = (pageRange == null || pageRange.getStartPageIndex() == null) ? 0 : pageRange.getStartPageIndex();
				int endPageIndex = (pageRange == null || pageRange.getEndPageIndex() == null) ? (pages.size() - 1) : pageRange.getEndPageIndex();

				JRPrintPage page = null;
				for(pageIndex = startPageIndex; pageIndex <= endPageIndex; pageIndex++)
				{
					if (Thread.interrupted())
					{
						throw new ExportInterruptedException();
					}

					page = pages.get(pageIndex);

					writer.write("<a name=\"" + JR_PAGE_ANCHOR_PREFIX + reportIndex + "_" + (pageIndex + 1) + "\"></a>\n");

					/*   */
					exportPage(page);

					if (reportIndex < items.size() - 1 || pageIndex < endPageIndex)
					{
						if (betweenPagesHtml == null)
						{
							writer.write("<br/>\n<br/>\n");
						}
						else
						{
							writer.write(betweenPagesHtml);
						}
					}

					writer.write("\n");
				}
			}
		}

		ReportContext reportContext = getReportContext();
		if (fontsToProcess != null && fontsToProcess.size() > 0)// when no resourceHandler, fonts are not processed 
		{
			if (reportContext == null) 
			{
				@SuppressWarnings("deprecation")
				HtmlResourceHandler resourceHandler = 
					getExporterOutput().getResourceHandler() == null
					? getResourceHandler()
					: getExporterOutput().getResourceHandler();

				for (HtmlFontFamily htmlFontFamily : fontsToProcess.values())
				{
					writer.write("<link class=\"jrWebFont\" rel=\"stylesheet\" href=\"" + resourceHandler.getResourcePath(htmlFontFamily.getId()) + "\">\n");
				}
				
				// generate script tag on static export only
				writer.write("<!--[if IE]>\n");
				writer.write("<script>\n");
				writer.write("var links = document.querySelectorAll('link.jrWebFont');\n");
				writer.write("setTimeout(function(){ if (links) { for (var i = 0; i < links.length; i++) { links.item(i).href = links.item(i).href; } } }, 0);\n");
				writer.write("</script>\n");
				writer.write("<![endif]-->\n");
			}
			else
			{
				reportContext.setParameterValue(JsonExporter.REPORT_CONTEXT_PARAMETER_WEB_FONTS, fontsToProcess);
			}
		}

		// place hyperlinksData on reportContext
		if (hyperlinksData.size() > 0) 
		{
			//for sure reportContext is not null, because otherwise there would be no item in the hyperilnkData
			reportContext.setParameterValue("net.sf.jasperreports.html.hyperlinks", hyperlinksData);
		}
		
		if (htmlFooter == null)
		{
			writer.write("</td><td width=\"50%\">&nbsp;</td></tr>\n");
			writer.write("</table>\n");
			writer.write("</body>\n");
			writer.write("</html>\n");
		}
		else
		{
			writer.write(htmlFooter);
		}

		if (flushOutput)
		{
			writer.flush();
		}
	}
	
	protected void exportPage(JRPrintPage page) throws IOException
	{
		Tabulator tabulator = new Tabulator(tableFilter, page.getElements());
		tabulator.tabulate();

		HtmlReportConfiguration configuration = getCurrentItemConfiguration(); 
		
		boolean isIgnorePageMargins = configuration.isIgnorePageMargins();
		if (!isIgnorePageMargins)
		{
			PrintPageFormat pageFormat = jasperPrint.getPageFormat(pageIndex);
			tabulator.addMargins(pageFormat.getPageWidth(), pageFormat.getPageHeight());
		}
		
		Table table = tabulator.getTable();
		
		boolean isWhitePageBackground = configuration.isWhitePageBackground();
		if (isWhitePageBackground)
		{
			setBackcolor(Color.white);
		}
		
		CellElementVisitor elementVisitor = new CellElementVisitor();
		TableVisitor tableVisitor = new TableVisitor(tabulator, elementVisitor);
		
		exportTable(tableVisitor, table, isWhitePageBackground, true);
		
		if (isWhitePageBackground)
		{
			restoreBackcolor();
		}
		
		JRExportProgressMonitor progressMonitor = configuration.getProgressMonitor();
		if (progressMonitor != null)
		{
			progressMonitor.afterPageExport();
		}
	}

	public void exportElements(List<JRPrintElement> elements) throws IOException
	{
		Tabulator tabulator = new Tabulator(tableFilter, elements);
		tabulator.tabulate();
		
		Table table = tabulator.getTable();
		
		CellElementVisitor elementVisitor = new CellElementVisitor();
		TableVisitor tableVisitor = new TableVisitor(tabulator, elementVisitor);
		
		exportTable(tableVisitor, table, false, false);
	}

	protected void exportTable(TableVisitor tableVisitor, Table table, boolean whiteBackground, boolean isMainReportTable) throws IOException
	{
		SortedSet<Column> columns = table.getColumns().getUserEntries();
		SortedSet<Row> rows = table.getRows().getUserEntries();
		if (columns.isEmpty() || rows.isEmpty())
		{
			// TODO lucianc empty page
			return;
		}
		

		if (isMainReportTable)
		{
			int totalWidth = columns.last().getEndCoord() - columns.first().getStartCoord();
			writer.write("<table class=\"jrPage\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"empty-cells: show; width: ");
			writer.write(toSizeUnit(totalWidth));
			writer.write(";");
		}
		else
		{
			writer.write("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"empty-cells: show; width: 100%;");
		}
		
		String borderCollapse = getCurrentItemConfiguration().getBorderCollapse();
		if (borderCollapse != null)
		{
			writer.write(" border-collapse: ");
			writer.write(borderCollapse);
			writer.write(";");
		}
		
		if (whiteBackground)
		{
			writer.write(" background-color: white;");
		}
		writer.write("\">\n");

		// TODO lucianc check whether we can use the first row for setting col widths
		writer.write("<tr valign=\"top\" style=\"height:0\">\n");
		for (Column col : columns)
		{
			writer.write("<td style=\"width:");
			writer.write(toSizeUnit(col.getExtent()));
			writer.write("\"></td>\n");
		}
		writer.write("</tr>\n");
		
		for (Row row : rows)
		{
			writer.write("<tr valign=\"top\" style=\"height:");
			writer.write(toSizeUnit(row.getExtent()));
			writer.write("\">\n");

			int emptySpan = 0;
			for (Column col : columns)
			{
				Cell cell = row.getCell(col);
				if (cell == null)
				{
					++emptySpan;
				}
				else
				{
					if (emptySpan > 0)
					{
						writeEmptyCell(emptySpan, 1);
					}
					emptySpan = 0;

					TablePosition position = new TablePosition(table, col, row);
					cell.accept(tableVisitor, position);
				}
			}
			if (emptySpan > 0)
			{
				writeEmptyCell(emptySpan, 1);
			}
			
			writer.write("</tr>\n");
		}
		
		writer.write("</table>\n");
	}

	protected void writeText(JRPrintText text, TableCell cell)
			throws IOException
	{
		JRStyledText styledText = getStyledText(text);
		int textLength = styledText == null ? 0 : styledText.length();
		
		startCell(text, cell);
		
		if (text.getRunDirectionValue() == RunDirectionEnum.RTL)
		{
			writer.write(" dir=\"rtl\"");
		}

		StringBuilder styleBuffer = new StringBuilder();

		String verticalAlignment = HTML_VERTICAL_ALIGN_TOP;

		switch (text.getVerticalTextAlign())
		{
			case BOTTOM :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_BOTTOM;
				break;
			}
			case MIDDLE :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_MIDDLE;
				break;
			}
			case TOP :
			case JUSTIFIED :
			default :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
			}
		}

		appendElementCellGenericStyle(cell, styleBuffer);
		appendBackcolorStyle(cell, styleBuffer);
		appendBorderStyle(cell.getBox(), styleBuffer);
		appendPaddingStyle(text.getLineBox(), styleBuffer);

		String horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
		if (textLength > 0)
		{
			switch (text.getHorizontalTextAlign())
			{
				case RIGHT :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_RIGHT;
					break;
				}
				case CENTER :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_CENTER;
					break;
				}
				case JUSTIFIED :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_JUSTIFY;
					break;
				}
				case LEFT :
				default :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
				}
			}
		}

		if (getCurrentItemConfiguration().isWrapBreakWord())
		{
			styleBuffer.append("width: " + toSizeUnit(text.getWidth()) + "; ");
			styleBuffer.append("word-wrap: break-word; ");
		}
		
		if (text.getLineBreakOffsets() != null)
		{
			//if we have line breaks saved in the text, set nowrap so that
			//the text only wraps at the explicit positions
			styleBuffer.append("white-space: nowrap; ");
		}
		
		styleBuffer.append("text-indent: " + text.getParagraph().getFirstLineIndent().intValue() + "px; ");

		String rotationValue = null;
		StringBuilder spanStyleBuffer = new StringBuilder();
		StringBuilder divStyleBuffer = new StringBuilder();
		if (text.getRotationValue() == RotationEnum.NONE)
		{
			if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
			{
				styleBuffer.append(" vertical-align: ");
				styleBuffer.append(verticalAlignment);
				styleBuffer.append(";");
			}

			//writing text align every time even when it's left
			//because IE8 with transitional defaults to center 
			styleBuffer.append("text-align: ");
			styleBuffer.append(horizontalAlignment);
			styleBuffer.append(";");
		}
		else
		{
			rotationValue = setRotationStyles(text, horizontalAlignment, 
					spanStyleBuffer, divStyleBuffer);
		}
		
		writeStyle(styleBuffer);
		
		finishStartCell();
		
		if (text.getAnchorName() != null)
		{
			writer.write("<a name=\"");
			writer.write(text.getAnchorName());
			writer.write("\"/>");
		}
		
		if (text.getBookmarkLevel() != JRAnchor.NO_BOOKMARK)
		{
			writer.write("<a name=\"");
			writer.write(JR_BOOKMARK_ANCHOR_PREFIX + reportIndex + "_" + pageIndex + "_" + cell.getElementAddress());
			writer.write("\"/>");
		}

		if (rotationValue != null)
		{
			writer.write("<div style=\"position: relative; overflow: hidden; ");
			writer.write(divStyleBuffer.toString());
			writer.write("\">\n");
			writer.write("<span class=\"rotated\" data-rotation=\"");
			writer.write(rotationValue);
			writer.write("\" style=\"position: absolute; display: table; ");
			writer.write(spanStyleBuffer.toString());
			writer.write("\">");
			writer.write("<span style=\"display: table-cell; vertical-align:"); //display:table-cell conflicts with overflow: hidden;
			writer.write(verticalAlignment);
			writer.write(";\">");
		}
		
		boolean hyperlinkStarted = startHyperlink(text);

		if (textLength > 0)
		{
			//only use text tooltip when no hyperlink present
//			String textTooltip = hyperlinkStarted ? null : text.getHyperlinkTooltip();
			exportStyledText(text, styledText, text.getHyperlinkTooltip(), hyperlinkStarted);
		}

		if (hyperlinkStarted)
		{
			endHyperlink();
		}
		
		if (rotationValue != null)
		{
			writer.write("</span></span></div>");
		}

		endCell();
	}

	protected String setRotationStyles(JRPrintText text, String horizontalAlignment, 
			StringBuilder spanStyleBuffer, StringBuilder divStyleBuffer)
	{
		String rotationValue;
		int textWidth = text.getWidth() - text.getLineBox().getLeftPadding() - text.getLineBox().getRightPadding();
		int textHeight = text.getHeight() - text.getLineBox().getTopPadding() - text.getLineBox().getBottomPadding();
		int rotatedWidth;
		int rotatedHeight;
		
		int rotationIE;
		int rotationAngle;
		int translateX;
		int translateY;
		switch (text.getRotationValue())
		{
			case LEFT : 
			{
				translateX = - (textHeight - textWidth) / 2;
				translateY = (textHeight - textWidth) / 2;
				rotatedWidth = textHeight;
				rotatedHeight = textWidth;
				rotationIE = 3;
				rotationAngle = -90;
				rotationValue = "left";
				break;
			}
			case RIGHT : 
			{
				translateX = - (textHeight - textWidth) / 2;
				translateY = (textHeight - textWidth) / 2;
				rotatedWidth = textHeight;
				rotatedHeight = textWidth;
				rotationIE = 1;
				rotationAngle = 90;
				rotationValue = "right";
				break;
			}
			case UPSIDE_DOWN : 
			{
				translateX = 0;
				translateY = 0;
				rotatedWidth = textWidth;
				rotatedHeight = textHeight;
				rotationIE = 2;
				rotationAngle = 180;
				rotationValue = "upsideDown";
				break;
			}
			case NONE :
			default :
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_UNEXPECTED_ROTATION_VALUE,  
						new Object[]{text.getRotationValue()}
						);
			}
		}

		appendSizeStyle(textWidth, textHeight, divStyleBuffer);
		appendSizeStyle(rotatedWidth, rotatedHeight, spanStyleBuffer);

		spanStyleBuffer.append("text-align: ");
		spanStyleBuffer.append(horizontalAlignment);
		spanStyleBuffer.append(";");
		
		spanStyleBuffer.append("-webkit-transform: translate(" + translateX + "px," + translateY + "px) ");
		spanStyleBuffer.append("rotate(" + rotationAngle + "deg); ");
		spanStyleBuffer.append("-moz-transform: translate(" + translateX + "px," + translateY + "px) ");
		spanStyleBuffer.append("rotate(" + rotationAngle + "deg); ");
		spanStyleBuffer.append("-ms-transform: translate(" + translateX + "px," + translateY + "px) ");
		spanStyleBuffer.append("rotate(" + rotationAngle + "deg); ");
		spanStyleBuffer.append("-o-transform: translate(" + translateX + "px," + translateY + "px) ");
		spanStyleBuffer.append("rotate(" + rotationAngle + "deg); ");
		spanStyleBuffer.append("filter: progid:DXImageTransform.Microsoft.BasicImage(rotation=" + rotationIE + "); ");
		return rotationValue;
	}

	protected void appendSizeStyle(int width, int height, StringBuilder styleBuffer)
	{
		styleBuffer.append("width:");
		styleBuffer.append(toSizeUnit(width));
		styleBuffer.append(";");

		styleBuffer.append("height:");
		styleBuffer.append(toSizeUnit(height));
		styleBuffer.append(";");
	}

	protected void writeImage(JRPrintImage image, TableCell cell)
			throws IOException, JRException
	{
		startCell(image, cell);

		int availableImageWidth = image.getWidth() - image.getLineBox().getLeftPadding() - image.getLineBox().getRightPadding();
		if (availableImageWidth < 0)
		{
			availableImageWidth = 0;
		}
	
		int availableImageHeight = image.getHeight() - image.getLineBox().getTopPadding() - image.getLineBox().getBottomPadding();
		if (availableImageHeight < 0)
		{
			availableImageHeight = 0;
		}

		String horizontalAlignment = getImageHorizontalAlignmentStyle(image);
		String verticalAlignment = getImageVerticalAlignmentStyle(image);

		StringBuilder styleBuffer = new StringBuilder();
		ScaleImageEnum scaleImage = image.getScaleImageValue();
		if (scaleImage != ScaleImageEnum.CLIP)
		{
			// clipped images are absolutely positioned within a div
			if (!horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
			{
				styleBuffer.append("text-align: ");
				styleBuffer.append(horizontalAlignment);
				styleBuffer.append(";");
			}

			if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
			{
				styleBuffer.append(" vertical-align: ");
				styleBuffer.append(verticalAlignment);
				styleBuffer.append(";");
			}
		}
		
		Renderable renderer = image.getRenderer();

		boolean isLazy = RendererUtil.isLazy(renderer);
		
		if (
			isLazy
			|| (scaleImage == ScaleImageEnum.CLIP && availableImageHeight > 0)
			)
		{
			// some browsers need td height so that height: 100% works on the div used for clipped images.
			// we're using the height without paddings because that's closest to the HTML size model.
			styleBuffer.append("height: ");
			styleBuffer.append(toSizeUnit(availableImageHeight));
			styleBuffer.append("; ");
		}

		appendElementCellGenericStyle(cell, styleBuffer);
		appendBackcolorStyle(cell, styleBuffer);
		
		boolean addedToStyle = appendBorderStyle(cell.getBox(), styleBuffer);
		if (!addedToStyle)
		{
			appendPen(
				styleBuffer,
				image.getLinePen(),
				null
				);
		}

		appendPaddingStyle(image.getLineBox(), styleBuffer);

		writeStyle(styleBuffer);

		finishStartCell();

		if (image.getAnchorName() != null)
		{
			writer.write("<a name=\"");
			writer.write(image.getAnchorName());
			writer.write("\"/>");
		}
		
		if (image.getBookmarkLevel() != JRAnchor.NO_BOOKMARK)
		{
			writer.write("<a name=\"");
			writer.write(JR_BOOKMARK_ANCHOR_PREFIX + reportIndex + "_" + pageIndex + "_" + cell.getElementAddress());
			writer.write("\"/>");
		}
		
		if (renderer != null)
		{
			boolean startedDiv = false;
			if (
				scaleImage == ScaleImageEnum.CLIP
				|| (isLazy 
				&& ((scaleImage == ScaleImageEnum.RETAIN_SHAPE || scaleImage == ScaleImageEnum.REAL_HEIGHT || scaleImage == ScaleImageEnum.REAL_SIZE) 
					|| (image.getHorizontalImageAlign() != HorizontalImageAlignEnum.LEFT || image.getVerticalImageAlign() != VerticalImageAlignEnum.TOP)))
				)
			{
				writer.write("<div style=\"width: 100%; height: 100%; position: relative; overflow: hidden;\">\n");
				startedDiv = true;
			}
			
			boolean hasAreaHyperlinks = 
				renderer instanceof AreaHyperlinksRenderable
				&& ((AreaHyperlinksRenderable)renderer).hasImageAreaHyperlinks();

			boolean hasHyperlinks = false;

			boolean hyperlinkStarted;
			if (hasAreaHyperlinks)
			{
				hyperlinkStarted = false;
				hasHyperlinks = true;
			}
			else
			{
				hyperlinkStarted = startHyperlink(image);
				hasHyperlinks = hyperlinkStarted;
			}
			
			String imageMapName = null;
			List<JRPrintImageAreaHyperlink> imageMapAreas = null;

			if (hasAreaHyperlinks)
			{
				Rectangle renderingArea = new Rectangle(image.getWidth(), image.getHeight());
				
				if (renderer instanceof DataRenderable)
				{
					imageMapName = imageMaps.get(new Pair<String, Rectangle>(renderer.getId(), renderingArea));
				}

				if (imageMapName == null)
				{
					Renderable originalRenderer = image.getRenderer();
					imageMapName = "map_" + getElementIndex(cell).toString() + "-" + originalRenderer.getId();//use renderer.getId()?
					imageMapAreas = ((AreaHyperlinksRenderable) originalRenderer).getImageAreaHyperlinks(renderingArea);//FIXMECHART
					
					if (renderer instanceof DataRenderable)
					{
						imageMaps.put(new Pair<String, Rectangle>(renderer.getId(), renderingArea), imageMapName);
					}
				}
			}

			boolean useBackgroundLazyImage = 
				isLazy 
				&& ((scaleImage == ScaleImageEnum.RETAIN_SHAPE || scaleImage == ScaleImageEnum.REAL_HEIGHT || scaleImage == ScaleImageEnum.REAL_SIZE) 
					|| !(image.getHorizontalImageAlign() == HorizontalImageAlignEnum.LEFT && image.getVerticalImageAlign() == VerticalImageAlignEnum.TOP));
				
			InternalImageProcessor imageProcessor = 
				new InternalImageProcessor(
					image,
					isLazy,
					!useBackgroundLazyImage && scaleImage != ScaleImageEnum.FILL_FRAME && !isLazy,
					cell,
					availableImageWidth,
					availableImageHeight
					);
			
			InternalImageProcessorResult imageProcessorResult = null;
			
			try
			{
				imageProcessorResult = imageProcessor.process(renderer);
			}
			catch (Exception e)
			{
				Renderable onErrorRenderer = getRendererUtil().handleImageError(e, image.getOnErrorTypeValue());
				if (onErrorRenderer != null)
				{
					imageProcessorResult = imageProcessor.process(onErrorRenderer);
				}
			}
			
			if (imageProcessorResult != null)
			{
				if (useBackgroundLazyImage)
				{
					writer.write("<div style=\"width: 100%; height: 100%; background-image: url('");
					String imagePath = imageProcessorResult.imageSource;
					if (imagePath != null)
					{
						writer.write(imagePath);
					}
					writer.write(
						"'); background-repeat: no-repeat; background-position: " 
						+ horizontalAlignment + " " 
						+ (image.getVerticalImageAlign() == VerticalImageAlignEnum.MIDDLE ? "center" : verticalAlignment) 
						+ ";background-size: "
						);
				
					switch (scaleImage)
					{
						case FILL_FRAME :
						{
							writer.write("100% 100%");
							break;
						}
						case CLIP :
						{
							writer.write("auto");
							break;
						}
						case RETAIN_SHAPE :
						default :
						{
							writer.write("contain");
						}
					}
					writer.write(";\"></div>");
				}
				else if (imageProcessorResult.isEmbededSvgData)
				{
					writer.write("<svg");

					switch (scaleImage)
					{
						case FILL_FRAME :
						{
							Dimension2D dimension = imageProcessorResult.dimension;
							if (dimension != null)
							{
								writer.write(" viewBox=\"0 0 ");
								writer.write(String.valueOf(dimension.getWidth()));
								writer.write(" ");
								writer.write(String.valueOf(dimension.getHeight()));
								writer.write("\"");
							}
				
							writer.write(" width=\"");
							writer.write(String.valueOf(availableImageWidth));
							writer.write("\"");
							writer.write(" height=\"");
							writer.write(String.valueOf(availableImageHeight));
							writer.write("\"");
							writer.write(" preserveAspectRatio=\"none\"");

							break;
						}
						case CLIP :
						{
							double normalWidth = availableImageWidth;
							double normalHeight = availableImageHeight;
	
							Dimension2D dimension = imageProcessorResult.dimension;
							if (dimension != null)
							{
								normalWidth = dimension.getWidth();
								normalHeight = dimension.getHeight();
	
								writer.write(" viewBox=\"");
								writer.write(String.valueOf((int)(ImageUtil.getXAlignFactor(image) * (normalWidth - availableImageWidth))));
								writer.write(" ");
								writer.write(String.valueOf((int)(ImageUtil.getYAlignFactor(image) * (normalHeight - availableImageHeight))));
								writer.write(" ");
								writer.write(String.valueOf(availableImageWidth));
								writer.write(" ");
								writer.write(String.valueOf(availableImageHeight));
								writer.write("\"");
							}

							writer.write(" width=\"");
							writer.write(String.valueOf(availableImageWidth));
							writer.write("\"");
							writer.write(" height=\"");
							writer.write(String.valueOf(availableImageHeight));
							writer.write("\"");

							break;
						}
						case RETAIN_SHAPE :
						default :
						{
							//considering the IF above, if we get here, then for sure isLazy() is false, so we can ask the renderer for its dimension
							if (availableImageHeight > 0)
							{
								double normalWidth = availableImageWidth;
								double normalHeight = availableImageHeight;

								Dimension2D dimension = imageProcessorResult.dimension;
								if (dimension != null)
								{
									normalWidth = dimension.getWidth();
									normalHeight = dimension.getHeight();

									writer.write(" viewBox=\"0 0 ");
									writer.write(String.valueOf(normalWidth));
									writer.write(" ");
									writer.write(String.valueOf(normalHeight));
									writer.write("\"");
								}
								
								double ratio = normalWidth / normalHeight;
				
								if ( ratio > (double)availableImageWidth / (double)availableImageHeight )
								{
									writer.write(" width=\"");
									writer.write(String.valueOf(availableImageWidth));
									writer.write("\"");
								}
								else
								{
									writer.write(" height=\"");
									writer.write(String.valueOf(availableImageHeight));
									writer.write("\"");
								}
							}
						}
					}

					writer.write("><g>\n");
					writer.write(imageProcessorResult.imageSource);
					writer.write("</g></svg>");
				}
				else
				{
					writer.write("<img");
					writer.write(" src=\"");
					String imagePath = imageProcessorResult.imageSource;
					if (imagePath != null)
					{
						writer.write(imagePath);
					}
					writer.write("\"");
				
					switch (scaleImage)
					{
						case FILL_FRAME :
						{
							writer.write(" style=\"width: ");
							writer.write(toSizeUnit(availableImageWidth));
							writer.write("; height: ");
							writer.write(toSizeUnit(availableImageHeight));
							writer.write("\"");
				
							break;
						}
						case CLIP :
						{
							int positionLeft;
							int positionTop;
							
							HorizontalImageAlignEnum horizontalAlign = image.getHorizontalImageAlign();
							VerticalImageAlignEnum verticalAlign = image.getVerticalImageAlign();
							if (  
								isLazy
								|| (horizontalAlign == HorizontalImageAlignEnum.LEFT && verticalAlign == VerticalImageAlignEnum.TOP)
								)
							{
								// no need to compute anything
								positionLeft = 0;
								positionTop = 0;
							}
							else
							{
								double normalWidth = availableImageWidth;
								double normalHeight = availableImageHeight;

								Dimension2D dimension = imageProcessorResult.dimension;
								if (dimension != null)
								{
									normalWidth = dimension.getWidth();
									normalHeight = dimension.getHeight();
								}
								
								// these calculations assume that the image td does not stretch due to other cells.
								// when that happens, the image will not be properly aligned.
								positionLeft = (int) (ImageUtil.getXAlignFactor(horizontalAlign) * (availableImageWidth - normalWidth));
								positionTop = (int) (ImageUtil.getYAlignFactor(verticalAlign) * (availableImageHeight - normalHeight));
							}
							
							writer.write(" style=\"position: absolute; left:");
							writer.write(toSizeUnit(positionLeft));
							writer.write("; top: ");
							writer.write(toSizeUnit(positionTop));
							// not setting width, height and clip as it doesn't seem needed plus it fixes clip for lazy images
							writer.write(";\"");

							break;
						}
						case RETAIN_SHAPE :
						default :
						{
							//considering the IF above, if we get here, then for sure isLazy() is false, so we can ask the renderer for its dimension
							if (availableImageHeight > 0)
							{
								double normalWidth = availableImageWidth;
								double normalHeight = availableImageHeight;

								Dimension2D dimension = imageProcessorResult.dimension;
								if (dimension != null)
								{
									normalWidth = dimension.getWidth();
									normalHeight = dimension.getHeight();
								}
								
								double ratio = normalWidth / normalHeight;
				
								if ( ratio > (double)availableImageWidth / (double)availableImageHeight )
								{
									writer.write(" style=\"width: ");
									writer.write(toSizeUnit(availableImageWidth));
									writer.write("\"");
								}
								else
								{
									writer.write(" style=\"height: ");
									writer.write(toSizeUnit(availableImageHeight));
									writer.write("\"");
								}
							}
						}
					}
					
					if (imageMapName != null)
					{
						writer.write(" usemap=\"#" + imageMapName + "\"");
					}
					
					writer.write(" alt=\"\"");
					
					if (hasHyperlinks)
					{
						writer.write(" border=\"0\"");
					}
					
					if (image.getHyperlinkTooltip() != null)
					{
						writer.write(" title=\"");
						writer.write(JRStringUtil.xmlEncode(image.getHyperlinkTooltip()));
						writer.write("\"");
					}
					
					writer.write("/>");
				}
			}

			if (hyperlinkStarted)
			{
				endHyperlink();
			}
			
			if (startedDiv)
			{
				writer.write("</div>");
			}
			
			if (imageMapAreas != null)
			{
				writer.write("\n");
				writeImageMap(imageMapName, image, imageMapAreas);
			}
		}
		
		endCell();
	}

	
	private class InternalImageProcessor
	{
		private final JRPrintElement imageElement;
		private final boolean isLazy; 
		private final boolean embedImage; 
		private final boolean needDimension; 
		private final TableCell cell;
		private final int availableImageWidth;
		private final int availableImageHeight;

		protected InternalImageProcessor(
			JRPrintElement imageElement,
			boolean isLazy,
			boolean needDimension, 
			TableCell cell,
			int availableImageWidth,
			int availableImageHeight
			)
		{
			this.imageElement = imageElement;
			this.isLazy = isLazy;
			this.embedImage = isEmbedImage(imageElement);
			this.needDimension = needDimension;
			this.cell = cell;
			this.availableImageWidth = availableImageWidth;
			this.availableImageHeight = availableImageHeight;
		}
		
		protected InternalImageProcessorResult process(Renderable renderer) throws JRException, IOException
		{
			String imageSource = null;
			Dimension2D dimension = null;
			boolean isEmbededSvgData = false;
			
			if (isLazy)
			{
				// we do not cache imagePath for lazy images because the short location string is already cached inside the render itself
				imageSource = RendererUtil.getResourceLocation(renderer);
			}
			else
			{
				if (renderer instanceof ResourceRenderer)
				{
					renderer = renderersCache.getLoadedRenderer((ResourceRenderer)renderer);
				}

				// check dimension first, to avoid caching renderers that might not be used eventually, due to their dimension errors 
				if (needDimension)
				{
					DimensionRenderable dimensionRenderer = renderersCache.getDimensionRenderable(renderer);
					dimension = dimensionRenderer == null ? null :  dimensionRenderer.getDimension(jasperReportsContext);
				}

				if (
					!embedImage //we do not cache imagePath for embedded images because it is too big
					&& renderer instanceof DataRenderable //we do not cache imagePath for non-data renderers because they render width different width/height each time
					&& rendererToImagePathMap.containsKey(renderer.getId())
					)
				{
					imageSource = rendererToImagePathMap.get(renderer.getId());
				}
				else
				{
					if (embedImage)
					{
						DataRenderable dataRenderer = 
							getRendererUtil().getDataRenderable(
								renderer,
								new Dimension(availableImageWidth, availableImageHeight),
								ModeEnum.OPAQUE == imageElement.getModeValue() ? imageElement.getBackcolor() : null
								);

						byte[] imageData = dataRenderer.getData(jasperReportsContext);

						isEmbededSvgData = getRendererUtil().isSvgData(imageData);
						
						if (isEmbededSvgData)
						{
							if (isEmbeddedSvgUseFonts(imageElement))
							{
								Locale locale = getLocale();

								SvgFontProcessor svgFontProcessor = new SvgFontProcessor(jasperReportsContext, locale) 
								{
									@Override
									public String getFontFamily(String fontFamily, Locale locale) 
									{
										// Here we rely on the ability of FontUtil.getFontInfoIgnoreCase(fontFamily, locale) method to
										// find fonts from font extensions based on the java.awt.Font.getFamily() of their font faces.
										// This is because the SVG produced by Batik stores the family name of the AWT fonts used to
										// render text on the Batik Graphics2D implementation, as it knows nothing about family names from JR extensions.
										return HtmlExporter.this.getFontFamily(true, fontFamily, locale);
									}
								};
								
								imageData = svgFontProcessor.process(imageData);
							}
							
							String encoding = getExporterOutput().getEncoding();
							
							imageSource = new String(imageData, encoding);
							
							// we might have received needDimension false above, as a hint, but if we arrive here, 
							// we definitely need to attempt getting the dimension of the SVG, regardless of scale image type
							DimensionRenderable dimensionRenderer = renderersCache.getDimensionRenderable(renderer);
							dimension = dimensionRenderer == null ? null :  dimensionRenderer.getDimension(jasperReportsContext);
						}
						else
						{
							String imageMimeType = 
								isEmbededSvgData
								? RendererUtil.SVG_MIME_TYPE
								: JRTypeSniffer.getImageTypeValue(imageData).getMimeType();

							ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							
							Base64Encoder encoder = new Base64Encoder(bais, baos);
							encoder.process();
							
							String encoding = getExporterOutput().getEncoding();
							
							imageSource = "data:" + imageMimeType + ";base64," + new String(baos.toByteArray(), encoding);
						}
						
						//don't cache embedded imageSource as they are not image paths
					}
					else
					{
						@SuppressWarnings("deprecation")
						HtmlResourceHandler imageHandler = 
							getImageHandler() == null 
							? getExporterOutput().getImageHandler() 
							: getImageHandler();
						if (imageHandler != null)
						{
							DataRenderable dataRenderer = 
								getRendererUtil().getDataRenderable(
									renderer,
									new Dimension(availableImageWidth, availableImageHeight),
									ModeEnum.OPAQUE == imageElement.getModeValue() ? imageElement.getBackcolor() : null
									);

							byte[] imageData = dataRenderer.getData(jasperReportsContext);
							
							String fileExtension = 
								getRendererUtil().isSvgData(imageData)
								? RendererUtil.SVG_FILE_EXTENSION
								: JRTypeSniffer.getImageTypeValue(imageData).getFileExtension();

							String imageName = getImageName(getElementIndex(cell), fileExtension);

							imageHandler.handleResource(imageName, imageData);
							
							imageSource = imageHandler.getResourcePath(imageName);

							if (dataRenderer == renderer)
							{
								//cache imagePath only for true ImageRenderable instances because the wrapping ones render with different width/height each time
								rendererToImagePathMap.put(renderer.getId(), imageSource);
							}
						}
						//does not make sense to cache null imagePath, in the absence of an image handler
					}
				}
			}
			
			return 
				new InternalImageProcessorResult(
					imageSource, 
					dimension,
					isEmbededSvgData
					);
		}
	}
	
	
	private static class InternalImageProcessorResult
	{
		protected final String imageSource;
		protected final Dimension2D dimension;
		protected final boolean isEmbededSvgData;
		
		protected InternalImageProcessorResult(
			String imagePath, 
			Dimension2D dimension,
			boolean isEmbededSvgData
			)
		{
			this.imageSource = imagePath;
			this.dimension = dimension;
			this.isEmbededSvgData = isEmbededSvgData;
		}
	}


	protected String getImageHorizontalAlignmentStyle(JRPrintImage image)
	{
		String horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
		switch (image.getHorizontalImageAlign())
		{
			case RIGHT :
			{
				horizontalAlignment = CSS_TEXT_ALIGN_RIGHT;
				break;
			}
			case CENTER :
			{
				horizontalAlignment = CSS_TEXT_ALIGN_CENTER;
				break;
			}
			case LEFT :
			default :
			{
				horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
			}
		}
		return horizontalAlignment;
	}

	protected String getImageVerticalAlignmentStyle(JRPrintImage image)
	{
		String verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
		switch (image.getVerticalImageAlign())
		{
			case BOTTOM :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_BOTTOM;
				break;
			}
			case MIDDLE :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_MIDDLE;
				break;
			}
			case TOP :
			default :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
			}
		}
		return verticalAlignment;
	}

	protected JRPrintElementIndex getElementIndex(TableCell cell)
	{
		String elementAddress = cell.getElementAddress();
		JRPrintElementIndex elementIndex = new JRPrintElementIndex(reportIndex, pageIndex,
						elementAddress);
		return elementIndex;
	}

	protected void writeImageMap(String imageMapName, JRPrintImage image, List<JRPrintImageAreaHyperlink> imageMapAreas) throws IOException
	{
		writer.write("<map name=\"" + imageMapName + "\">\n");

		for (ListIterator<JRPrintImageAreaHyperlink> it = imageMapAreas.listIterator(imageMapAreas.size()); it.hasPrevious();)
		{
			JRPrintImageAreaHyperlink areaHyperlink = it.previous();
			JRPrintHyperlink link = areaHyperlink.getHyperlink();
			JRPrintImageArea area = areaHyperlink.getArea();

			writer.write("  <area shape=\"" + JRPrintImageArea.getHtmlShape(area.getShape()) + "\"");
			
			writeImageAreaCoordinates(area.getCoordinates());			
			writeImageAreaHyperlink(link);
			writer.write("/>\n");
		}
		
		if (image.getHyperlinkTypeValue() != HyperlinkTypeEnum.NONE)
		{
			writer.write("  <area shape=\"default\"");
			writeImageAreaCoordinates(new int[]{0, 0, image.getWidth(), image.getHeight()});//for IE
			writeImageAreaHyperlink(image);
			writer.write("/>\n");
		}
		
		writer.write("</map>\n");
	}
	
	protected void writeImageAreaCoordinates(int[] coords) throws IOException
	{
		if (coords != null && coords.length > 0)
		{
			StringBuilder coordsEnum = new StringBuilder(coords.length * 4);
			coordsEnum.append(toZoom(coords[0]));
			for (int i = 1; i < coords.length; i++)
			{
				coordsEnum.append(',');
				coordsEnum.append(toZoom(coords[i]));
			}
			writer.write(" coords=\"" + coordsEnum + "\"");
		}		
	}


	protected void writeImageAreaHyperlink(JRPrintHyperlink hyperlink) throws IOException
	{
		if (getReportContext() != null)
		{
			if (hyperlink.getLinkType() != null)
			{
				int id = hyperlink.hashCode() & 0x7FFFFFFF;
				writer.write(" class=\"_jrHyperLink " + hyperlink.getLinkType() + "\" data-id=\"" + id + "\"");

				HyperlinkData hyperlinkData = new HyperlinkData();
				hyperlinkData.setId(String.valueOf(id));
				hyperlinkData.setHref(getHyperlinkURL(hyperlink));
				hyperlinkData.setSelector("._jrHyperLink." + hyperlink.getLinkType());
				hyperlinkData.setHyperlink(hyperlink);

				hyperlinksData.add(hyperlinkData);
			}
		}
		else
		{
			String href = getHyperlinkURL(hyperlink);
			if (href == null)
			{
				writer.write(" nohref=\"nohref\"");
			}
			else
			{
				writer.write(" href=\"" + href + "\"");

				String target = getHyperlinkTarget(hyperlink);
				if (target != null)
				{
					writer.write(" target=\"");
					writer.write(target);
					writer.write("\"");
				}
			}
		}

		if (hyperlink.getHyperlinkTooltip() != null)
		{
			writer.write(" title=\"");
			writer.write(JRStringUtil.xmlEncode(hyperlink.getHyperlinkTooltip()));
			writer.write("\"");
		}
	}

	protected void writeRectangle(JRPrintRectangle rectangle, TableCell cell) throws IOException
	{
		startCell(rectangle, cell);
		
		int radius = rectangle.getRadius();
		if (radius == 0)
		{
			StringBuilder styleBuffer = new StringBuilder();
			appendElementCellGenericStyle(cell, styleBuffer);
			appendBackcolorStyle(cell, styleBuffer);
			appendPen(
				styleBuffer,
				rectangle.getLinePen(),
				null
				);
			writeStyle(styleBuffer);
		}

		finishStartCell();

		if (radius != 0)
		{
			float lineDiff = rectangle.getLinePen().getLineWidth() / 2;
			writer.write("<svg height=\"" + rectangle.getHeight() + "\" width=\"" + rectangle.getWidth() + "\">");
			writer.write("<rect x=\"" + lineDiff + "\" y=\"" + lineDiff + "\" rx=\"" + radius + "\" ry=\"" + radius + "\" ");
			writer.write("height=\"" + (rectangle.getHeight() - 2 * lineDiff) + "\" width=\"" + (rectangle.getWidth() - 2 * lineDiff) + "\" ");
			writeSvgStyle(rectangle);
			writer.write("\"/></svg>");
		}

		endCell();
	}

	protected void writeEllipse(JRPrintEllipse ellipse, TableCell cell) throws IOException
	{
		startCell(ellipse, cell);

		finishStartCell();

		float lineDiff = ellipse.getLinePen().getLineWidth() / 2;
		writer.write("<svg height=\"" + ellipse.getHeight() + "\" width=\"" + ellipse.getWidth() + "\">");
		writer.write("<ellipse cx=\"" + (ellipse.getWidth() / 2) + "\" cy=\"" + (ellipse.getHeight() / 2));
		writer.write("\" rx=\"" + (ellipse.getWidth() / 2 - lineDiff) + "\" ry=\"" + (ellipse.getHeight() / 2 - lineDiff) + "\" ");
		writeSvgStyle(ellipse);
		writer.write("\"/></svg>");
		
		endCell();
	}

	protected void writeSvgStyle(JRPrintGraphicElement element) throws IOException
	{
		writer.write("style=\"fill:" + JRColorUtil.getCssColor(element.getBackcolor()) + ";");
		writer.write("stroke:" + JRColorUtil.getCssColor(element.getLinePen().getLineColor()) + ";");
		writer.write("stroke-width:" + element.getLinePen().getLineWidth() + ";");

		switch (element.getLinePen().getLineStyleValue())
		{
			case DOTTED :
			{
				writer.write("stroke-dasharray:" + element.getLinePen().getLineWidth() + "," + element.getLinePen().getLineWidth() + ";");
				break;
			}
			case DASHED :
			{
				writer.write("stroke-dasharray:" + 5 * element.getLinePen().getLineWidth() + "," + 3 * element.getLinePen().getLineWidth() + ";");
				break;
			}
			case DOUBLE : //FIXME: there is no built-in svg support for double stroke style; strokes could be rendered twice as a workaround
			case SOLID :
			default :
			{
				break;
			}
		}
	}

	protected void writeLine(JRPrintLine line, TableCell cell)
			throws IOException
	{		
		startCell(line, cell);
		finishStartCell();
		
		int width = line.getWidth();
		int height = line.getHeight();
		LineDirectionEnum lineDirection = line.getDirectionValue();
		int y1 = lineDirection == LineDirectionEnum.BOTTOM_UP ? height : 0;
		int y2 = lineDirection == LineDirectionEnum.BOTTOM_UP ? 0 : height;
		
		writer.write("<svg height=\"" + height + "\" width=\"" + width + "\">");
		writer.write("<line x1=\"0\" y1=\"" + y1 +"\" x2=\"" + width + "\" y2=\"" + y2 + "\" ");
		writeSvgStyle(line);
		writer.write("\"/></svg>");
		
		endCell();
	}
	
	protected void writeGenericElement(JRGenericPrintElement element, TableCell cell) throws IOException
	{
		GenericElementHtmlHandler handler = (GenericElementHtmlHandler) 
				GenericElementHandlerEnviroment.getInstance(getJasperReportsContext()).getElementHandler(
						element.getGenericType(), HTML_EXPORTER_KEY);
		
		if (handler == null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("No HTML generic element handler for " 
						+ element.getGenericType());
			}
			
			writeEmptyCell(cell.getColumnSpan(), cell.getRowSpan());// TODO lucianc backcolor/borders?
		}
		else
		{
			startCell(element, cell);

			StringBuilder styleBuffer = new StringBuilder();
			appendElementCellGenericStyle(cell, styleBuffer);
			appendBackcolorStyle(cell, styleBuffer);
			appendBorderStyle(cell.getBox(), styleBuffer);
			if (styleBuffer.length() > 0)
			{
				writer.write(" style=\"");
				writer.write(styleBuffer.toString());
				writer.write("\"");
			}

			finishStartCell();
			
			String htmlFragment = handler.getHtmlFragment(exporterContext, element);
			if (htmlFragment != null)
			{
				writer.write(htmlFragment);
			}
			
			endCell();
		}
	}
	
	protected void writeLayers(List<Table> layers, TableVisitor tableVisitor, TableCell cell) throws IOException
	{
		startCell(cell);

		StringBuilder styleBuffer = new StringBuilder();
		appendElementCellGenericStyle(cell, styleBuffer);
		appendBackcolorStyle(cell, styleBuffer);
		appendBorderStyle(cell.getBox(), styleBuffer);
		writeStyle(styleBuffer);

		finishStartCell();
		
		// layers need to always specify backcolors
		setBackcolor(null);
		writer.write("<div style=\"width: 100%; height: 100%; position: relative;\">\n");

		for (ListIterator<Table> it = layers.listIterator(); it.hasNext();)
		{
			Table table = it.next();
			
			StringBuilder layerStyleBuffer = new StringBuilder();
			if (it.hasNext()) {
				layerStyleBuffer.append("position: absolute; overflow: hidden; ");
			} else {
				layerStyleBuffer.append("position: relative; ");
			}
			layerStyleBuffer.append("width: 100%; height: 100%; ");
			
			if (it.previousIndex() > 0) {
				layerStyleBuffer.append("pointer-events: none; ");
			}

			writer.write("<div style=\"");
			writer.write(layerStyleBuffer.toString());
			writer.write("\">\n");

			++pointerEventsNoneStack;
			exportTable(tableVisitor, table, false, false);
			--pointerEventsNoneStack;
			
			writer.write("</div>\n");
		}
		
		writer.write("</div>\n");
		restoreBackcolor();

		endCell();
	}

	protected void startCell(JRPrintElement element, TableCell cell) throws IOException
	{
		startCell(cell.getColumnSpan(), cell.getRowSpan());

		String dataAttr = getDataAttributes(element, cell);
		if (dataAttr != null)
		{
			writer.write(dataAttr);
		}
	}
	
	public String getDataAttributes(JRPrintElement element, TableCell cell)
	{
		StringBuilder sb = new StringBuilder();
		
		String id = getCellProperty(element, cell, PROPERTY_HTML_ID);
		if (id != null)
		{
			sb.append(" id=\"" + id +"\"");
		}
		String clazz = getCellProperty(element, cell, PROPERTY_HTML_CLASS);
		if (clazz != null)
		{
			sb.append(" class=\"" + clazz +"\"");
		}
		String colUuid = getCellProperty(element, cell, HeaderToolbarElement.PROPERTY_COLUMN_UUID);//FIXMEJIVE register properties like this in a pluggable way; extensions?
		if (colUuid != null)
		{
			sb.append(" data-coluuid=\"" + colUuid + "\"");
		}
		String cellId = getCellProperty(element, cell, HeaderToolbarElement.PROPERTY_CELL_ID);
		if (cellId != null)
		{
			sb.append(" data-cellid=\"" + cellId + "\"");
		}
		String tableUuid = getCellProperty(element, cell, HeaderToolbarElement.PROPERTY_TABLE_UUID);
		if (tableUuid != null)
		{
			sb.append(" data-tableuuid=\"" + tableUuid + "\"");
		}
		String columnIndex = getCellProperty(element, cell, HeaderToolbarElement.PROPERTY_COLUMN_INDEX);
		if (columnIndex != null)
		{
			sb.append(" data-colidx=\"" + columnIndex + "\"");
		}
		
		String xtabId = getCellProperty(element, cell, CrosstabInteractiveJsonHandler.PROPERTY_CROSSTAB_ID);
		if (xtabId != null)
		{
			sb.append(" " + CrosstabInteractiveJsonHandler.ATTRIBUTE_CROSSTAB_ID + "=\"" 
					+ JRStringUtil.htmlEncode(xtabId) + "\"");
		}
		
		String xtabColIdx = getCellProperty(element, cell, CrosstabInteractiveJsonHandler.PROPERTY_COLUMN_INDEX);
		if (xtabColIdx != null)
		{
			sb.append(" " + CrosstabInteractiveJsonHandler.ATTRIBUTE_COLUMN_INDEX + "=\"" 
					+ JRStringUtil.htmlEncode(xtabColIdx) + "\"");
		}
		
		return sb.length() > 0 ? sb.toString() : null;
	}
	
	protected String getCellProperty(JRPrintElement element, TableCell cell, String key)
	{
		String property = null;
		if (element != null)
		{
			property = getPropertiesUtil().getProperty(element, key);
		}
		
		if (property == null)
		{
			Tabulator tabulator = cell.getTabulator();
			for (FrameCell parentCell = cell.getCell().getParent(); 
					parentCell != null && property == null;
					parentCell = parentCell.getParent())
			{
				JRPrintElement parentElement = tabulator.getCellElement(parentCell);
				property = getPropertiesUtil().getProperty(parentElement, key);
			}
		}
		return property;
	}
	
	protected void startCell(TableCell cell) throws IOException
	{
		startCell(cell.getElement(), cell);
	}

	protected void startCell(int colSpan, int rowSpan) throws IOException
	{
		writer.write("<td");
		if (colSpan > 1)
		{
			writer.write(" colspan=\"");
			writer.write(Integer.toString(colSpan));
			writer.write("\"");
		}
		if (rowSpan > 1)
		{
			writer.write(" rowspan=\"");
			writer.write(Integer.toString(rowSpan));
			writer.write("\"");
		}		
	}
	
	protected void finishStartCell() throws IOException
	{
		writer.write(">\n");
	}
	
	protected void endCell() throws IOException
	{
		writer.write("</td>\n");
	}
	
	protected void writeEmptyCell(int colSpan, int rowSpan) throws IOException
	{
		startCell(colSpan, rowSpan);
		finishStartCell();
		endCell();
	}
	
	protected void writeFrameCell(TableCell cell) throws IOException
	{
		startCell(cell);
		
		StringBuilder styleBuffer = new StringBuilder();
		appendElementCellGenericStyle(cell, styleBuffer);
		appendBackcolorStyle(cell, styleBuffer);
		appendBorderStyle(cell.getBox(), styleBuffer);
		writeStyle(styleBuffer);

		finishStartCell();
		endCell();
	}

	protected void writeStyle(StringBuilder styleBuffer) throws IOException
	{
		if (styleBuffer.length() > 0)
		{
			writer.write(" style=\"");
			writer.write(styleBuffer.toString());
			writer.write("\"");
		}
	}
	
	protected void appendElementCellGenericStyle(TableCell cell, StringBuilder styleBuffer)
	{
		if (pointerEventsNoneStack > 0 && cell.getElement() != null)
		{
			styleBuffer.append("pointer-events: auto; ");
		}
	}

	protected void setBackcolor(Color color)
	{
		backcolorStack.addFirst(color);
	}

	protected void restoreBackcolor()
	{
		backcolorStack.removeFirst();
	}

	protected boolean matchesBackcolor(Color backcolor)
	{
		if (backcolorStack.isEmpty())
		{
			return false;
		}
		
		Color currentBackcolor = backcolorStack.getFirst();
		return currentBackcolor != null && backcolor.getRGB() == currentBackcolor.getRGB();
	}
	
	protected Color appendBackcolorStyle(TableCell cell, StringBuilder styleBuffer)
	{
		Color cellBackcolor = cell.getBackcolor();
		if (cellBackcolor != null && !matchesBackcolor(cellBackcolor))
		{
			styleBuffer.append("background-color: ");
			styleBuffer.append(JRColorUtil.getCssColor(cellBackcolor));
			styleBuffer.append("; ");

			return cellBackcolor;
		}

		return null;
	}

	protected boolean appendBorderStyle(JRLineBox box, StringBuilder styleBuffer)
	{
		boolean addedToStyle = false;

		if (box != null)
		{
			LineStyleEnum tps = box.getTopPen().getLineStyleValue();
			LineStyleEnum lps = box.getLeftPen().getLineStyleValue();
			LineStyleEnum bps = box.getBottomPen().getLineStyleValue();
			LineStyleEnum rps = box.getRightPen().getLineStyleValue();
			
			float tpw = box.getTopPen().getLineWidth().floatValue();
			float lpw = box.getLeftPen().getLineWidth().floatValue();
			float bpw = box.getBottomPen().getLineWidth().floatValue();
			float rpw = box.getRightPen().getLineWidth().floatValue();
			
			if (0f < tpw && tpw < 1f) {
				tpw = 1f;
			}
			if (0f < lpw && lpw < 1f) {
				lpw = 1f;
			}
			if (0f < bpw && bpw < 1f) {
				bpw = 1f;
			}
			if (0f < rpw && rpw < 1f) {
				rpw = 1f;
			}
			
			Color tpc = box.getTopPen().getLineColor();
			
			// try to compact all borders into one css property
			if (tps == lps &&												// same line style
					tps == bps &&
					tps == rps &&
					tpw == lpw &&											// same line width
					tpw == bpw &&
					tpw == rpw &&
					tpc.equals(box.getLeftPen().getLineColor()) &&			// same line color
					tpc.equals(box.getBottomPen().getLineColor()) &&
					tpc.equals(box.getRightPen().getLineColor())) 
			{
				addedToStyle |= appendPen(
						styleBuffer,
						box.getTopPen(),
						null
						);
			} else {
				addedToStyle |= appendPen(
					styleBuffer,
					box.getTopPen(),
					"top"
					);
				addedToStyle |= appendPen(
					styleBuffer,
					box.getLeftPen(),
					"left"
					);
				addedToStyle |= appendPen(
					styleBuffer,
					box.getBottomPen(),
					"bottom"
					);
				addedToStyle |= appendPen(
					styleBuffer,
					box.getRightPen(),
					"right"
					);
			}
		}
		
		return addedToStyle;
	}
	
	protected boolean appendPen(StringBuilder sb, JRPen pen, String side)
	{
		boolean addedToStyle = false;
		
		float borderWidth = pen.getLineWidth().floatValue();
		if (0f < borderWidth && borderWidth < 1f)
		{
			borderWidth = 1f;
		}

		String borderStyle = null;
		switch (pen.getLineStyleValue())
		{
			case DOUBLE :
			{
				borderStyle = "double";
				break;
			}
			case DOTTED :
			{
				borderStyle = "dotted";
				break;
			}
			case DASHED :
			{
				borderStyle = "dashed";
				break;
			}
			case SOLID :
			default :
			{
				borderStyle = "solid";
				break;
			}
		}

		if (borderWidth > 0f)
		{
			sb.append("border");
			if (side != null)
			{
				sb.append("-");
				sb.append(side);
			}

			sb.append(": ");
			sb.append(toSizeUnit((int)borderWidth));
			
			sb.append(" ");
			sb.append(borderStyle);

			sb.append(" ");
			sb.append(JRColorUtil.getCssColor(pen.getLineColor()));
			sb.append("; ");

			addedToStyle = true;
		}

		return addedToStyle;
	}
	
	protected boolean appendPaddingStyle(JRLineBox box, StringBuilder styleBuffer)
	{
		boolean addedToStyle = false;
		
		if (box != null)
		{
			Integer tp = box.getTopPadding();
			Integer lp = box.getLeftPadding();
			Integer bp = box.getBottomPadding();
			Integer rp = box.getRightPadding();
			
			// try to compact all paddings into one css property
			if (tp == lp && tp == bp && tp == rp)
			{
				addedToStyle |= appendPadding(
						styleBuffer,
						tp,
						null
						);
			} else 
			{
				addedToStyle |= appendPadding(
						styleBuffer,
						box.getTopPadding(),
						"top"
						);
				addedToStyle |= appendPadding(
						styleBuffer,
						box.getLeftPadding(),
						"left"
						);
				addedToStyle |= appendPadding(
						styleBuffer,
						box.getBottomPadding(),
						"bottom"
						);
				addedToStyle |= appendPadding(
						styleBuffer,
						box.getRightPadding(),
						"right"
						);
			}
		}
		
		return addedToStyle;
	}
	
	protected boolean appendPadding(StringBuilder sb, Integer padding, String side)
	{
		boolean addedToStyle = false;
		
		if (padding.intValue() > 0)
		{
			sb.append("padding");
			if (side != null)
			{
				sb.append("-");
				sb.append(side);
			}
			sb.append(": ");
			sb.append(toSizeUnit(padding.intValue()));
			sb.append("; ");

			addedToStyle = true;
		}
		
		return addedToStyle;
	}

	protected boolean startHyperlink(JRPrintHyperlink link) throws IOException
	{
		boolean hyperlinkStarted = false,
				canWrite = false;

		if (getReportContext() != null)
		{
			Boolean ignoreHyperlink = HyperlinkUtil.getIgnoreHyperlink(HtmlReportConfiguration.PROPERTY_IGNORE_HYPERLINK, link);
			if (ignoreHyperlink == null)
			{
				ignoreHyperlink = getCurrentItemConfiguration().isIgnoreHyperlink();
			}

			if (!ignoreHyperlink && link.getLinkType() != null)
			{
				canWrite = true;
				int id = link.hashCode() & 0x7FFFFFFF;

				writer.write("<span class=\"_jrHyperLink " + link.getLinkType() + "\" data-id=\"" + id + "\"");

				HyperlinkData hyperlinkData = new HyperlinkData();
				hyperlinkData.setId(String.valueOf(id));
				hyperlinkData.setHref(getHyperlinkURL(link));
				hyperlinkData.setSelector("._jrHyperLink." + link.getLinkType());
				hyperlinkData.setHyperlink(link);

				hyperlinksData.add(hyperlinkData);
				hyperlinkStarted = true;
			}
		}
		else
		{
			String href = getHyperlinkURL(link);

			if (href != null)
			{
				canWrite = true;
				writer.write("<a href=\"");
				writer.write(href);
				writer.write("\"");

				String target = getHyperlinkTarget(link);
				if (target != null)
				{
					writer.write(" target=\"");
					writer.write(target);
					writer.write("\"");
				}
			}

			hyperlinkStarted = href != null;
		}

		if (canWrite)
		{
			if (link.getHyperlinkTooltip() != null)
			{
				writer.write(" title=\"");
				writer.write(JRStringUtil.xmlEncode(link.getHyperlinkTooltip()));
				writer.write("\"");
			}

			writer.write(">");
		}

		return hyperlinkStarted;
	}

	protected void endHyperlink() throws IOException
	{
		if (getReportContext() != null) {
			writer.write("</span>");
		}
		else
		{
			writer.write("</a>");
		}
	}

	protected String getHyperlinkURL(JRPrintHyperlink link)
	{
		return resolveHyperlinkURL(reportIndex, link);
	}
	
	protected String resolveHyperlinkURL(int reportIndex, JRPrintHyperlink link)
	{
		String href = null;
		
		Boolean ignoreHyperlink = HyperlinkUtil.getIgnoreHyperlink(HtmlReportConfiguration.PROPERTY_IGNORE_HYPERLINK, link);
		if (ignoreHyperlink == null)
		{
			ignoreHyperlink = getCurrentItemConfiguration().isIgnoreHyperlink();
		}

		if (!ignoreHyperlink)
		{
			JRHyperlinkProducer customHandler = getHyperlinkProducer(link);		
			if (customHandler == null)
			{
				switch(link.getHyperlinkTypeValue())
				{
					case REFERENCE :
					{
						if (link.getHyperlinkReference() != null)
						{
							href = link.getHyperlinkReference();
						}
						break;
					}
					case LOCAL_ANCHOR :
					{
						if (link.getHyperlinkAnchor() != null)
						{
							href = "#" + link.getHyperlinkAnchor();
						}
						break;
					}
					case LOCAL_PAGE :
					{
						if (link.getHyperlinkPage() != null)
						{
							href = "#" + JR_PAGE_ANCHOR_PREFIX + reportIndex + "_" + link.getHyperlinkPage().toString();
						}
						break;
					}
					case REMOTE_ANCHOR :
					{
						if (
							link.getHyperlinkReference() != null &&
							link.getHyperlinkAnchor() != null
							)
						{
							href = link.getHyperlinkReference() + "#" + link.getHyperlinkAnchor();
						}
						break;
					}
					case REMOTE_PAGE :
					{
						if (
							link.getHyperlinkReference() != null &&
							link.getHyperlinkPage() != null
							)
						{
							href = link.getHyperlinkReference() + "#" + JR_PAGE_ANCHOR_PREFIX + "0_" + link.getHyperlinkPage().toString();
						}
						break;
					}
					case NONE :
					default :
					{
						break;
					}
				}
			}
			else
			{
				href = customHandler.getHyperlink(link);
			}
		}
		
		return href;
	}

	protected String getHyperlinkTarget(JRPrintHyperlink link)
	{
		String target = null;
		JRHyperlinkTargetProducer producer = targetProducerFactory.getHyperlinkTargetProducer(link.getLinkTarget());		
		if (producer == null)
		{
			switch(link.getHyperlinkTargetValue())
			{
				case BLANK :
				{
					target = "_blank";//FIXME make reverse for html markup hyperlinks
					break;
				}
				case PARENT :
				{
					target = "_parent";
					break;
				}
				case TOP :
				{
					target = "_top";
					break;
				}
				case CUSTOM :
				{
					boolean paramFound = false;
					List<JRPrintHyperlinkParameter> parameters = link.getHyperlinkParameters() == null ? null : link.getHyperlinkParameters().getParameters();
					if (parameters != null)
					{
						for(Iterator<JRPrintHyperlinkParameter> it = parameters.iterator(); it.hasNext();)
						{
							JRPrintHyperlinkParameter parameter = it.next();
							if (link.getLinkTarget().equals(parameter.getName()))
							{
								target = parameter.getValue() == null ? null : parameter.getValue().toString();
								paramFound = true;
								break;
							}
						}
					}
					if (!paramFound)
					{
						target = link.getLinkTarget();
					}
					break;
				}
				case SELF :
				default :
				{
				}
			}
		}
		else
		{
			target = producer.getHyperlinkTarget(link);
		}

		return target;
	}

	public String toSizeUnit(float size)
	{
		Number number = toZoom(size);
		if (number.intValue() == number.floatValue())
		{
			number = number.intValue();
		}

		return String.valueOf(number) + getCurrentItemConfiguration().getSizeUnit().getName();
	}

	/**
	 * @deprecated Replaced by {@link #toSizeUnit(float)}.
	 */
	public String toSizeUnit(int size)
	{
		return toSizeUnit((float)size);
	}

	protected float toZoom(float size)//FIXMEEXPORT cache this
	{
		float zoom = DEFAULT_ZOOM;
		
		Float zoomRatio = getCurrentItemConfiguration().getZoomRatio();
		if (zoomRatio != null)
		{
			zoom = zoomRatio.floatValue();
			if (zoom <= 0)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_INVALID_ZOOM_RATIO,  
						new Object[]{zoom} 
						);
			}
		}

		return (zoom * size);
	}

	/**
	 * @deprecated Replaced by {@link #toZoom(float)}.
	 */
	protected int toZoom(int size)
	{
		return (int)toZoom((float)size);
	}

	@Override
	protected JRStyledText getStyledText(JRPrintText textElement, boolean setBackcolor)
	{
		JRStyledText styledText = super.getStyledText(textElement, setBackcolor);
		
		if (styledText != null)
		{
			short[] lineBreakOffsets = textElement.getLineBreakOffsets();
			if (lineBreakOffsets != null && lineBreakOffsets.length > 0)
			{
				//insert new lines at the line break positions saved at fill time
				//cloning the text first
				//FIXME do we need this?  styled text instances are no longer shared
				styledText = styledText.cloneText();
				styledText.insert("\n", lineBreakOffsets);
			}
		}
		
		return styledText;
	}

	private void addSearchAttributes(JRStyledText styledText, JRPrintText textElement) {
		ReportContext reportContext = getReportContext();
		if (reportContext != null) {
			SpansInfo spansInfo = (SpansInfo) reportContext.getParameterValue("net.sf.jasperreports.search.term.highlighter");
			PrintElementId pei = PrintElementId.forElement(textElement);

			if (spansInfo != null && spansInfo.hasHitTermsInfo(pei.toString())) {
				List<HitTermInfo> hitTermInfos = JRCloneUtils.cloneList(spansInfo.getHitTermsInfo(pei.toString()));

				short[] lineBreakOffsets = textElement.getLineBreakOffsets();
				if (lineBreakOffsets != null && lineBreakOffsets.length > 0) {
					int sz = lineBreakOffsets.length;
					for (HitTermInfo ti: hitTermInfos) {
						for (int i = 0; i < sz; i++) {
							if (lineBreakOffsets[i] <= ti.getStart()) {
								ti.setStart(ti.getStart() + 1);
								ti.setEnd(ti.getEnd() + 1);
							} else {
								break;
							}
						}
					}
				}

				AttributedString attributedString = styledText.getAttributedString();
				for (int i = 0, ln = hitTermInfos.size(); i < ln; i = i + spansInfo.getTermsPerQuery()) {
					attributedString.addAttribute(JRTextAttribute.SEARCH_HIGHLIGHT, Color.yellow, hitTermInfos.get(i).getStart(), hitTermInfos.get(i + spansInfo.getTermsPerQuery() - 1).getEnd());
				}
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No ReportContext to hold search data!");
			}
		}
	}
	
	protected void exportStyledText(JRPrintText printText, JRStyledText styledText, String tooltip, boolean hyperlinkStarted) throws IOException
	{
		Locale locale = getTextLocale(printText);
		LineSpacingEnum lineSpacing = printText.getParagraph().getLineSpacing();
		Float lineSpacingSize = printText.getParagraph().getLineSpacingSize();
		float lineSpacingFactor = printText.getLineSpacingFactor();
		Color backcolor = printText.getBackcolor();
		
		String text = styledText.getText();

		int runLimit = 0;

		addSearchAttributes(styledText, printText);

		AttributedCharacterIterator iterator = styledText.getAttributedString().getIterator();

		boolean first = true;
		boolean startedSpan = false;

		boolean highlightStarted = false;

		while(runLimit < styledText.length() && (runLimit = iterator.getRunLimit()) <= styledText.length())
		{
			//if there are several text runs, write the tooltip into a parent <span>
			if (first && runLimit < styledText.length() && tooltip != null)
			{
				startedSpan = true;
				writer.write("<span title=\"");
				writer.write(JRStringUtil.xmlEncode(tooltip));
				writer.write("\">");
				//reset the tooltip so that inner <span>s to not use it
				tooltip = null;
			}
			first = false;

			Map<Attribute,Object> attributes = iterator.getAttributes();
			Color highlightColor = (Color) attributes.get(JRTextAttribute.SEARCH_HIGHLIGHT);
			if (highlightColor != null && !highlightStarted) {
				highlightStarted = true;
				writer.write("<span class=\"jr_search_result\">");
			} else if (highlightColor == null && highlightStarted) {
				highlightStarted = false;
				writer.write("</span>");
			}

			exportStyledTextRun(
				attributes,
				text.substring(iterator.getIndex(), runLimit),
				tooltip,
				locale,
				lineSpacing,
				lineSpacingSize,
				lineSpacingFactor,
				backcolor,
				hyperlinkStarted
			);

			iterator.setIndex(runLimit);
		}

		if (highlightStarted) {
			writer.write("</span>");
		}
		
		if (startedSpan)
		{
			writer.write("</span>");
		}
	}
	
	protected void exportStyledTextRun(
			Map<Attribute,Object> attributes,
			String text,
			String tooltip,
			Locale locale,
			LineSpacingEnum lineSpacing,
			Float lineSpacingSize,
			float lineSpacingFactor,
			Color backcolor,
			boolean hyperlinkStarted
			) throws IOException
	{
		boolean localHyperlink = false;
		JRPrintHyperlink hyperlink = (JRPrintHyperlink)attributes.get(JRTextAttribute.HYPERLINK);
		if (!hyperlinkStarted && hyperlink != null)
		{
			localHyperlink = startHyperlink(hyperlink);
		}

		boolean isBold = TextAttribute.WEIGHT_BOLD.equals(attributes.get(TextAttribute.WEIGHT));
		boolean isItalic = TextAttribute.POSTURE_OBLIQUE.equals(attributes.get(TextAttribute.POSTURE));

		String fontFamilyAttr = (String)attributes.get(TextAttribute.FAMILY);
		String fontFamily = getFontFamily(false, fontFamilyAttr, locale);

		// do not put single quotes around family name here because the value might already contain quotes, 
		// especially if it is coming from font extension export configuration
		writer.write("<span style=\"font-family: ");
		writer.write(fontFamily);
		writer.write("; ");

		Color forecolor = (Color)attributes.get(TextAttribute.FOREGROUND);
		if (!hyperlinkStarted || !Color.black.equals(forecolor))
		{
			writer.write("color: ");
			writer.write(JRColorUtil.getCssColor(forecolor));
			writer.write("; ");
		}

		Color runBackcolor = (Color)attributes.get(TextAttribute.BACKGROUND);
		if (runBackcolor != null && !runBackcolor.equals(backcolor))
		{
			writer.write("background-color: ");
			writer.write(JRColorUtil.getCssColor(runBackcolor));
			writer.write("; ");
		}

		writer.write("font-size: ");
		writer.write(toSizeUnit((Float)attributes.get(TextAttribute.SIZE)));
		writer.write(";");
			
		switch (lineSpacing)
		{
			case SINGLE:
			default:
			{
				if (lineSpacingFactor == 0)
				{
					writer.write(" line-height: 1; *line-height: normal;");
				}
				else
				{
					writer.write(" line-height: " + lineSpacingFactor + ";");
				}
				break;
			}
			case ONE_AND_HALF:
			{
				if (lineSpacingFactor == 0)
				{
					writer.write(" line-height: 1.5;");
				}
				else
				{
					writer.write(" line-height: " + lineSpacingFactor + ";");
				}
				break;
			}
			case DOUBLE:
			{
				if (lineSpacingFactor == 0)
				{
					writer.write(" line-height: 2.0;");
				}
				else
				{
					writer.write(" line-height: " + lineSpacingFactor + ";");
				}
				break;
			}
			case PROPORTIONAL:
			{
				if (lineSpacingSize != null) {
					writer.write(" line-height: " + lineSpacingSize.floatValue() + ";");
				}
				break;
			}
			case AT_LEAST:
			case FIXED:
			{
				if (lineSpacingSize != null) {
					writer.write(" line-height: " + lineSpacingSize.floatValue() + "px;");
				}
				break;
			}
		}

		/*
		if (!horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
		{
			writer.write(" text-align: ");
			writer.write(horizontalAlignment);
			writer.write(";");
		}
		*/

		if (isBold)
		{
			writer.write(" font-weight: bold;");
		}
		if (isItalic)
		{
			writer.write(" font-style: italic;");
		}
		if (TextAttribute.UNDERLINE_ON.equals(attributes.get(TextAttribute.UNDERLINE)))
		{
			writer.write(" text-decoration: underline;");
		}
		if (TextAttribute.STRIKETHROUGH_ON.equals(attributes.get(TextAttribute.STRIKETHROUGH)))
		{
			writer.write(" text-decoration: line-through;");
		}

		if (TextAttribute.SUPERSCRIPT_SUPER.equals(attributes.get(TextAttribute.SUPERSCRIPT)))
		{
			writer.write(" vertical-align: super;");
		}
		else if (TextAttribute.SUPERSCRIPT_SUB.equals(attributes.get(TextAttribute.SUPERSCRIPT)))
		{
			writer.write(" vertical-align: sub;");
		}
			
		writer.write("\"");

		if (tooltip != null)
		{
			writer.write(" title=\"");
			writer.write(JRStringUtil.xmlEncode(tooltip));
			writer.write("\"");
		}
			
		writer.write(">");

		writer.write(
			JRStringUtil.htmlEncode(text)
			);

		writer.write("</span>");

		if (localHyperlink)
		{
			endHyperlink();
		}
	}

	protected class TableVisitor implements CellVisitor<TablePosition, Void, IOException>
	{
		private final Tabulator tabulator;
		private final PrintElementVisitor<TableCell> elementVisitor;
		
		public TableVisitor(Tabulator tabulator, PrintElementVisitor<TableCell> elementVisitor)
		{
			this.tabulator = tabulator;
			this.elementVisitor = elementVisitor;
		}
		
		@Override
		public Void visit(ElementCell cell, TablePosition position)
		{
			TableCell tableCell = tabulator.getTableCell(position, cell);
			JRPrintElement element = tableCell.getElement();
			element.accept(elementVisitor, tableCell);
			return null;
		}

		@Override
		public Void visit(SplitCell cell, TablePosition position)
		{
			//NOP
			return null;
		}

		@Override
		public Void visit(FrameCell frameCell, TablePosition position) throws IOException
		{
			TableCell tableCell = tabulator.getTableCell(position, frameCell);
			HtmlExporter.this.writeFrameCell(tableCell);
			return null;
		}

		@Override
		public Void visit(LayeredCell layeredCell, TablePosition position)
				throws IOException
		{
			TableCell tableCell = tabulator.getTableCell(position, layeredCell);
			HtmlExporter.this.writeLayers(layeredCell.getLayers(), this, tableCell);
			return null;
		}
	}

	protected class CellElementVisitor implements PrintElementVisitor<TableCell>
	{
		@Override
		public void visit(JRPrintText textElement, TableCell cell)
		{
			try
			{
				writeText(textElement, cell);
			}
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
		}

		@Override
		public void visit(JRPrintImage image, TableCell cell)
		{
			try
			{
				writeImage(image, cell);
			}
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
		}

		@Override
		public void visit(JRPrintRectangle rectangle, TableCell cell)
		{
			try
			{
				writeRectangle(rectangle, cell);
			} 
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
		}

		@Override
		public void visit(JRPrintLine line, TableCell cell)
		{
			try
			{
				writeLine(line, cell);
			}
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
		}

		@Override
		public void visit(JRPrintEllipse ellipse, TableCell cell)
		{
			try
			{
				writeEllipse(ellipse, cell);
			} 
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
		}

		@Override
		public void visit(JRPrintFrame frame, TableCell cell)
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_INTERNAL_ERROR,  
					(Object[])null 
					);
		}

		@Override
		public void visit(JRGenericPrintElement printElement, TableCell cell)
		{
			try
			{
				writeGenericElement(printElement, cell);
			} 
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
		}
	}

	protected class ExporterContext extends BaseExporterContext implements JRHtmlExporterContext
	{
		@Override
		public String getHyperlinkURL(JRPrintHyperlink link)
		{
			return HtmlExporter.this.getHyperlinkURL(link);
		}
	}
}
