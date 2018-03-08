<?xml version="1.0" encoding="utf-8"?>
<!--
  Copyright 2007 Red Hat, Inc.
  License: GPL
  Author: Jeff Fearn <jfearn@redhat.com>
  Author: Tammy Fox <tfox@redhat.com>
  Author: Andy Fitzsimon <afitzsim@redhat.com>
  Author: Mark Newton <mark.newton@jboss.org>
  Author: Pete Muir
-->

<!DOCTYPE xsl:stylesheet [
    <!ENTITY lowercase "'abcdefghijklmnopqrstuvwxyz'">
    <!ENTITY uppercase "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'">
    ]>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:d="http://docbook.org/ns/docbook"
                version="1.0"
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:jbh="java:org.jboss.highlight.renderer.FORenderer"
                exclude-result-prefixes="jbh">

  <xsl:import href="http://docbook.sourceforge.net/release/xsl/1.72.0/fo/docbook.xsl"/>

  <xsl:import href="common.xsl"/>

  <xsl:param name="alignment">justify</xsl:param>
  <xsl:param name="fop.extensions" select="1"/>
  <xsl:param name="fop1.extensions" select="0"/>
  <xsl:param name="img.src.path"/>
  <xsl:param name="qandadiv.autolabel" select="0"/>

  <xsl:param name="hyphenation-character">-</xsl:param>
  <!--xsl:param name="hyphenate.verbatim" select="0"/-->
  <xsl:param name="hyphenate">true</xsl:param>
  <!--xsl:param name="ulink.hyphenate" select="1"/-->

  <xsl:param name="line-height" select="1.5"/>

  <!-- Callouts -->
  <!-- Place callout bullets at this column in programlisting.-->
  <xsl:param name="callout.defaultcolumn">80</xsl:param>
  <xsl:param name="callout.icon.size">10pt</xsl:param>

  <!-- Admonitions -->
  <xsl:param name="admon.graphics.extension" select="'.png'"/>

  <xsl:attribute-set name="admonition.title.properties">
    <xsl:attribute name="font-size">13pt</xsl:attribute>
    <xsl:attribute name="color">
      <xsl:choose>
        <xsl:when test="self::d:note">#4C5253</xsl:when>
        <xsl:when test="self::d:caution">#533500</xsl:when>
        <xsl:when test="self::d:important">white</xsl:when>
        <xsl:when test="self::d:warning">white</xsl:when>
        <xsl:when test="self::d:tip">white</xsl:when>
        <xsl:otherwise>white</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>

    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="hyphenate">false</xsl:attribute>
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>

  </xsl:attribute-set>

  <xsl:attribute-set name="graphical.admonition.properties">

    <xsl:attribute name="color">
      <xsl:choose>
        <xsl:when test="self::d:note">#4C5253</xsl:when>
        <xsl:when test="self::d:caution">#533500</xsl:when>
        <xsl:when test="self::d:important">white</xsl:when>
        <xsl:when test="self::d:warning">white</xsl:when>
        <xsl:when test="self::d:tip">white</xsl:when>
        <xsl:otherwise>white</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="background-color">
      <xsl:choose>
        <xsl:when test="self::d:note">#B5BCBD</xsl:when>
        <xsl:when test="self::d:caution">#E3A835</xsl:when>
        <xsl:when test="self::d:important">#4A5D75</xsl:when>
        <xsl:when test="self::d:warning">#7B1E1E</xsl:when>
        <xsl:when test="self::d:tip">#7E917F</xsl:when>
        <xsl:otherwise>#404040</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>

    <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">1.2em</xsl:attribute>
    <xsl:attribute name="space-after.optimum">1em</xsl:attribute>
    <xsl:attribute name="space-after.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-after.maximum">1em</xsl:attribute>
    <xsl:attribute name="padding-bottom">12pt</xsl:attribute>
    <xsl:attribute name="padding-top">12pt</xsl:attribute>
    <xsl:attribute name="padding-right">12pt</xsl:attribute>
    <xsl:attribute name="padding-left">12pt</xsl:attribute>
    <xsl:attribute name="margin-left">
      <xsl:value-of select="$title.margin.left"/>
    </xsl:attribute>
  </xsl:attribute-set>

  <!-- Old non-monospace fonts for programlisting -->
  <!-- <xsl:param name="programlisting.font" select="'verdana,helvetica,sans-serif'" /> -->
  <!-- <xsl:param name="programlisting.font.size" select="'100%'" /> -->

  <!-- Change to monospace font for programlisting, needed to workaround crappy callouts -->
  <xsl:param name="programlisting.font" select="$monospace.font.family"/>
  <!-- Make the font for programlisting slightly smaller -->
  <xsl:param name="programlisting.font.size" select="'90%'"/>

  <!-- Make the section depth in the TOC 2, same as html -->
  <xsl:param name="toc.section.depth">2</xsl:param>

  <!-- Now, set enable scalefit for large images -->
  <xsl:param name="graphicsize.extension" select="'1'"/>
  <xsl:param name="default.image.width">17.4cm</xsl:param>

  <xsl:attribute-set name="xref.properties">
    <xsl:attribute name="font-style">italic</xsl:attribute>
    <xsl:attribute name="color">
      <xsl:choose>
        <xsl:when test="ancestor::d:note or ancestor::d:caution or ancestor::d:important or ancestor::d:warning or ancestor::d:tip">
          <xsl:text>#aee6ff</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>#0066cc</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="monospace.properties">
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="font-family">
      <xsl:value-of select="$monospace.font.family"/>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="monospace.verbatim.properties" use-attribute-sets="verbatim.properties monospace.properties">
    <xsl:attribute name="text-align">start</xsl:attribute>
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">&#x25BA;</xsl:attribute>
  </xsl:attribute-set>

  <xsl:param name="shade.verbatim" select="1"/>
  <xsl:attribute-set name="shade.verbatim.style">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="background-color">
      <xsl:choose>
        <xsl:when test="ancestor::d:note">
          <xsl:text>#D6DEE0</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:caution">
          <xsl:text>#FAF8ED</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:important">
          <xsl:text>#E1EEF4</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:warning">
          <xsl:text>#FAF8ED</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:tip">
          <xsl:text>#D5E1D5</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>#EDE7C8</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="color">
      <xsl:choose>
        <xsl:when test="ancestor::d:note">
          <xsl:text>#334558</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:caution">
          <xsl:text>#334558</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:important">
          <xsl:text>#334558</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:warning">
          <xsl:text>#334558</xsl:text>
        </xsl:when>
        <xsl:when test="ancestor::d:tip">
          <xsl:text>#334558</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>#333</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="padding-left">12pt</xsl:attribute>
    <xsl:attribute name="padding-right">12pt</xsl:attribute>
    <xsl:attribute name="padding-top">6pt</xsl:attribute>
    <xsl:attribute name="padding-bottom">6pt</xsl:attribute>
    <xsl:attribute name="margin-left">
      <xsl:value-of select="$title.margin.left"/>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="verbatim.properties">
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-width">.3mm</xsl:attribute>
    <xsl:attribute name="border-color">#E1E9EB</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">1.2em</xsl:attribute>
    <xsl:attribute name="space-after.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-after.optimum">1em</xsl:attribute>
    <xsl:attribute name="space-after.maximum">1.2em</xsl:attribute>
    <xsl:attribute name="hyphenate">false</xsl:attribute>
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="white-space-collapse">false</xsl:attribute>
    <xsl:attribute name="white-space-treatment">preserve</xsl:attribute>
    <xsl:attribute name="linefeed-treatment">preserve</xsl:attribute>
    <xsl:attribute name="text-align">start</xsl:attribute>
  </xsl:attribute-set>

  <xsl:param name="generate.toc">
    set toc
    book toc
    article toc
  </xsl:param>

  <!--###################################################
 Custom TOC (bold chapter titles)
 ################################################### -->

  <!-- Improve the TOC. -->
  <xsl:template name="toc.line">
    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>

    <xsl:variable name="label">
      <xsl:apply-templates select="." mode="label.markup"/>
    </xsl:variable>

    <fo:block text-align-last="justify" end-indent="{$toc.indent.width}pt"
              last-line-end-indent="-{$toc.indent.width}pt">
      <fo:inline keep-with-next.within-line="always">
        <fo:basic-link internal-destination="{$id}">

          <!-- Chapter titles should be bold. -->
          <xsl:choose>
            <xsl:when test="local-name(.) = 'chapter'">
              <xsl:attribute name="font-weight">bold</xsl:attribute>
            </xsl:when>
          </xsl:choose>

          <xsl:if test="$label != ''">
            <xsl:copy-of select="$label"/>
            <xsl:value-of select="$autotoc.label.separator"/>
          </xsl:if>
          <xsl:apply-templates select="." mode="titleabbrev.markup"/>
        </fo:basic-link>
      </fo:inline>
      <fo:inline keep-together.within-line="always">
        <xsl:text> </xsl:text>
        <fo:leader leader-pattern="dots" leader-pattern-width="3pt"
                   leader-alignment="reference-area"
                   keep-with-next.within-line="always"/>
        <xsl:text> </xsl:text>
        <fo:basic-link internal-destination="{$id}">
          <fo:page-number-citation ref-id="{$id}"/>
        </fo:basic-link>
      </fo:inline>
    </fo:block>
  </xsl:template>

  <!-- Format Variable Lists as Blocks (prevents horizontal overflow). -->
  <xsl:param name="variablelist.as.blocks">1</xsl:param>

  <!-- The horrible list spacing problems, this is much better. -->
  <xsl:attribute-set name="list.block.spacing">
    <xsl:attribute name="space-before.optimum">2em</xsl:attribute>
    <xsl:attribute name="space-before.minimum">1em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">3em</xsl:attribute>
    <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
    <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
    <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
  </xsl:attribute-set>

  <!-- Some padding inside tables -->
  <xsl:attribute-set name="table.cell.padding">
    <xsl:attribute name="padding-left">4pt</xsl:attribute>
    <xsl:attribute name="padding-right">4pt</xsl:attribute>
    <xsl:attribute name="padding-top">2pt</xsl:attribute>
    <xsl:attribute name="padding-bottom">2pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- Only hairlines as frame and cell borders in tables -->
  <xsl:param name="table.frame.border.thickness">0.3pt</xsl:param>
  <xsl:param name="table.cell.border.thickness">0.15pt</xsl:param>
  <xsl:param name="table.cell.border.color">#5c5c4f</xsl:param>
  <xsl:param name="table.frame.border.color">#5c5c4f</xsl:param>
  <xsl:param name="table.cell.border.right.color">white</xsl:param>
  <xsl:param name="table.cell.border.left.color">white</xsl:param>
  <xsl:param name="table.frame.border.right.color">white</xsl:param>
  <xsl:param name="table.frame.border.left.color">white</xsl:param>
  <!-- Paper type, no headers on blank pages, no double sided printing -->
  <xsl:param name="paper.type" select="'A4'"/>
  <xsl:param name="double.sided">1</xsl:param>
  <xsl:param name="headers.on.blank.pages">1</xsl:param>
  <xsl:param name="footers.on.blank.pages">1</xsl:param>
  <!--xsl:param name="header.column.widths" select="'1 4 1'"/-->
  <xsl:param name="header.column.widths" select="'1 0 1'"/>
  <xsl:param name="footer.column.widths" select="'1 1 1'"/>
  <xsl:param name="header.rule" select="1"/>

  <!-- Space between paper border and content (chaotic stuff, don't touch) -->
  <xsl:param name="page.margin.top">15mm</xsl:param>
  <xsl:param name="region.before.extent">10mm</xsl:param>
  <xsl:param name="body.margin.top">15mm</xsl:param>

  <xsl:param name="body.margin.bottom">15mm</xsl:param>
  <xsl:param name="region.after.extent">10mm</xsl:param>
  <xsl:param name="page.margin.bottom">15mm</xsl:param>

  <xsl:param name="page.margin.outer">30mm</xsl:param>
  <xsl:param name="page.margin.inner">30mm</xsl:param>

  <!-- No intendation of Titles -->
  <xsl:param name="body.start.indent">0pt</xsl:param>

  <xsl:param name="title.color">#000000</xsl:param>
  <xsl:param name="chapter.title.color" select="$title.color"/>
  <xsl:param name="section.title.color" select="$title.color"/>

  <xsl:attribute-set name="section.title.level1.properties">
    <xsl:attribute name="color">
      <xsl:value-of select="$section.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.6"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="section.title.level2.properties">
    <xsl:attribute name="color">
      <xsl:value-of select="$section.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.4"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="section.title.level3.properties">
    <xsl:attribute name="color">
      <xsl:value-of select="$section.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.3"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="section.title.level4.properties">
    <xsl:attribute name="color">
      <xsl:value-of select="$section.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.2"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="section.title.level5.properties">
    <xsl:attribute name="color">
      <xsl:value-of select="$section.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.1"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="section.title.level6.properties">
    <xsl:attribute name="color">
      <xsl:value-of select="$section.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.properties">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$title.font.family"/>
    </xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <!-- font size is calculated dynamically by section.heading template -->
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">1.0em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">1.2em</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="start-indent">
      <xsl:value-of select="$title.margin.left"/>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:param name="titlepage.color" select="$title.color"/>

  <xsl:attribute-set name="book.titlepage.recto.style">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$title.fontset"/>
    </xsl:attribute>
    <xsl:attribute name="color">
      <xsl:value-of select="$titlepage.color"/>
    </xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <xsl:attribute name="text-align">center</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="component.title.properties">
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
    <xsl:attribute name="space-before.optimum">
      <xsl:value-of select="concat($body.font.master, 'pt')"/>
    </xsl:attribute>
    <xsl:attribute name="space-before.minimum">
      <xsl:value-of select="concat($body.font.master, 'pt')"/>
    </xsl:attribute>
    <xsl:attribute name="space-before.maximum">
      <xsl:value-of select="concat($body.font.master, 'pt')"/>
    </xsl:attribute>
    <xsl:attribute name="hyphenate">false</xsl:attribute>
    <xsl:attribute name="color">
      <xsl:choose>
        <xsl:when test="not(parent::d:chapter | parent::d:article | parent::d:appendix)">
          <xsl:value-of select="$title.color"/>
        </xsl:when>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="text-align">
      <xsl:choose>
        <xsl:when test="((parent::d:article | parent::d:articleinfo) and not(ancestor::d:book) and not(self::d:bibliography))         or (parent::d:slides | parent::d:slidesinfo)">center</xsl:when>
        <xsl:otherwise>left</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="start-indent">
      <xsl:value-of select="$title.margin.left"/>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="chapter.titlepage.recto.style">
    <xsl:attribute name="color">
      <xsl:value-of select="$chapter.title.color"/>
    </xsl:attribute>
    <xsl:attribute name="background-color">white</xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:choose>
        <xsl:when test="$l10n.gentext.language = 'ja-JP'">
          <xsl:value-of select="$body.font.master * 1.7"/>
          <xsl:text>pt</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>24pt</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <!--xsl:attribute name="wrap-option">no-wrap</xsl:attribute-->
    <xsl:attribute name="padding-left">1em</xsl:attribute>
    <xsl:attribute name="padding-right">1em</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="preface.titlepage.recto.style">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$title.fontset"/>
    </xsl:attribute>
    <xsl:attribute name="color">#000000</xsl:attribute>
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="part.titlepage.recto.style">
    <xsl:attribute name="color">
      <xsl:value-of select="$title.color"/>
    </xsl:attribute>
    <xsl:attribute name="text-align">center</xsl:attribute>
  </xsl:attribute-set>


  <!--
  From: fo/table.xsl
  Reason: Table Header format
  Version:1.72
  -->
  <xsl:template name="table.cell.block.properties">
    <!-- highlight this entry? -->
    <xsl:if test="ancestor::d:thead or ancestor::d:tfoot">
      <xsl:attribute name="font-weight">bold</xsl:attribute>
      <xsl:attribute name="background-color">#4a5d75</xsl:attribute>
      <xsl:attribute name="color">white</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!--
  From: fo/table.xsl
  Reason: Table Header format
  Version:1.72
  -->
  <!-- customize this template to add row properties -->
  <xsl:template name="table.row.properties">
    <xsl:variable name="bgcolor">
      <xsl:call-template name="dbfo-attribute">
        <xsl:with-param name="pis" select="processing-instruction('dbfo')"/>
        <xsl:with-param name="attribute" select="'bgcolor'"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$bgcolor != ''">
      <xsl:attribute name="background-color">
        <xsl:value-of select="$bgcolor"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="ancestor::d:thead or ancestor::d:tfoot">
      <xsl:attribute name="background-color">#4a5d75</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!--
  From: fo/titlepage.templates.xsl
  Reason: Switch to using chapter.titlepage.recto.style
  Version:1.72
  -->
  <xsl:template match="title" mode="appendix.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="chapter.titlepage.recto.style">
      <xsl:call-template name="component.title.nomarkup">
        <xsl:with-param name="node" select="ancestor-or-self::d:appendix[1]"/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <!--
  From: fo/titlepage.templates.xsl
  Reason: Remove font size and weight overrides
  Version:1.72
  -->
  <xsl:template match="title" mode="chapter.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="chapter.titlepage.recto.style">
      <xsl:value-of select="."/>
    </fo:block>
  </xsl:template>

  <!--
  From: fo/titlepage.templates.xsl
  Reason: Remove font family, size and weight overrides
  Version:1.72
  -->
  <xsl:template name="preface.titlepage.recto">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="preface.titlepage.recto.style" margin-left="{$title.margin.left}">
      <xsl:call-template name="component.title.nomarkup">
        <xsl:with-param name="node" select="ancestor-or-self::d:preface[1]"/>
      </xsl:call-template>
    </fo:block>
    <xsl:choose>
      <xsl:when test="d:prefaceinfo/d:subtitle">
        <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:docinfo/d:subtitle">
        <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:info/d:subtitle">
        <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:subtitle">
        <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:subtitle"/>
      </xsl:when>
    </xsl:choose>

    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:corpauthor"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:corpauthor"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:corpauthor"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:authorgroup"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:authorgroup"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:authorgroup"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:author"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:author"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:author"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:othercredit"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:othercredit"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:othercredit"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:releaseinfo"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:releaseinfo"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:releaseinfo"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:copyright"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:copyright"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:copyright"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:legalnotice"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:legalnotice"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:legalnotice"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:pubdate"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:pubdate"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:pubdate"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:revision"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:revision"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:revision"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:revhistory"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:revhistory"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:revhistory"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:prefaceinfo/d:abstract"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:docinfo/d:abstract"/>
    <xsl:apply-templates mode="preface.titlepage.recto.auto.mode" select="d:info/d:abstract"/>
  </xsl:template>


  <xsl:template name="pickfont-sans">
    <xsl:variable name="font">
      <xsl:choose>
        <xsl:when test="$l10n.gentext.language = 'ja-JP'">
          <xsl:text>KochiMincho,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'ko-KR'">
          <xsl:text>BaekmukBatang,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'zh-CN'">
          <xsl:text>ARPLKaitiMGB,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'bn-IN'">
          <xsl:text>LohitBengali,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'ta-IN'">
          <xsl:text>LohitTamil,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'pa-IN'">
          <xsl:text>LohitPunjabi,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'hi-IN'">
          <xsl:text>LohitHindi,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'gu-IN'">
          <xsl:text>LohitGujarati,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'zh-TW'">
          <xsl:text>ARPLMingti2LBig5,</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$fop1.extensions != 0">
        <xsl:copy-of select="$font"/>
        <xsl:text>DejaVuLGCSans,sans-serif</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$font"/>
        <xsl:text>sans-serif</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="pickfont-serif">
    <xsl:variable name="font">
      <xsl:choose>
        <xsl:when test="$l10n.gentext.language = 'ja-JP'">
          <xsl:text>KochiMincho,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'ko-KR'">
          <xsl:text>BaekmukBatang,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'zh-CN'">
          <xsl:text>ARPLKaitiMGB,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'bn-IN'">
          <xsl:text>LohitBengali,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'ta-IN'">
          <xsl:text>LohitTamil,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'pa-IN'">
          <xsl:text>LohitPunjabi,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'hi-IN'">
          <xsl:text>LohitHindi,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'gu-IN'">
          <xsl:text>LohitGujarati,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'zh-TW'">
          <xsl:text>ARPLMingti2LBig5,</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$fop1.extensions != 0">
        <xsl:copy-of select="$font"/>
        <xsl:text>DejaVuLGCSans,serif</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$font"/>
        <xsl:text>serif</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="pickfont-mono">
    <xsl:variable name="font">
      <xsl:choose>
        <xsl:when test="$l10n.gentext.language = 'ja-JP'">
          <xsl:text>KochiMincho,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'ko-KR'">
          <xsl:text>BaekmukBatang,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'zh-CN'">
          <xsl:text>ARPLKaitiMGB,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'bn-IN'">
          <xsl:text>LohitBengali,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'ta-IN'">
          <xsl:text>LohitTamil,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'pa-IN'">
          <xsl:text>LohitPunjabi,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'hi-IN'">
          <xsl:text>LohitHindi,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'gu-IN'">
          <xsl:text>LohitGujarati,</xsl:text>
        </xsl:when>
        <xsl:when test="$l10n.gentext.language = 'zh-TW'">
          <xsl:text>ARPLMingti2LBig5,</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$fop1.extensions != 0">
        <xsl:copy-of select="$font"/>
        <xsl:text>DejaVuLGCSans,monospace</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$font"/>
        <xsl:text>monospace</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--xsl:param name="symbol.font.family">
    <xsl:choose>
      <xsl:when test="$l10n.gentext.language = 'ja-JP'">
        <xsl:text>Symbol,ZapfDingbats</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>Symbol,ZapfDingbats</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param-->

  <xsl:param name="title.font.family">
    <xsl:call-template name="pickfont-sans"/>
  </xsl:param>

  <xsl:param name="body.font.family">
    <xsl:call-template name="pickfont-sans"/>
  </xsl:param>

  <xsl:param name="monospace.font.family">
    <xsl:call-template name="pickfont-mono"/>
  </xsl:param>

  <xsl:param name="sans.font.family">
    <xsl:call-template name="pickfont-sans"/>
  </xsl:param>

  <!--xsl:param name="callout.unicode.font">
    <xsl:call-template name="pickfont-sans"/>
  </xsl:param-->

  <!--
  From: fo/verbatim.xsl
  Reason: Left align address
  Version: 1.72
  -->

  <xsl:template match="d:address">
    <xsl:param name="suppress-numbers" select="'0'"/>

    <xsl:variable name="content">
      <xsl:choose>
        <xsl:when test="$suppress-numbers = '0'
                      and @linenumbering = 'numbered'
                      and $use.extensions != '0'
                      and $linenumbering.extension != '0'">
          <xsl:call-template name="number.rtf.lines">
            <xsl:with-param name="rtf">
              <xsl:apply-templates/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <fo:block wrap-option='no-wrap'
              white-space-collapse='false'
              white-space-treatment='preserve'
              linefeed-treatment="preserve"
              text-align="start"
              xsl:use-attribute-sets="verbatim.properties">
      <xsl:copy-of select="$content"/>
    </fo:block>
  </xsl:template>

  <xsl:template name="component.title.nomarkup">
    <xsl:param name="node" select="."/>

    <xsl:variable name="id">
      <xsl:call-template name="object.id">
        <xsl:with-param name="object" select="$node"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="title">
      <xsl:apply-templates select="$node" mode="object.title.markup">
        <xsl:with-param name="allow-anchors" select="1"/>
      </xsl:apply-templates>
    </xsl:variable>
    <xsl:copy-of select="$title"/>
  </xsl:template>

  <!--
  From: fo/pagesetup.xsl
  Reason: Custom Header
  Version: 1.72
  -->
  <xsl:template name="header.content">
    <xsl:param name="pageclass" select="''"/>
    <xsl:param name="sequence" select="''"/>
    <xsl:param name="position" select="''"/>
    <xsl:param name="gentext-key" select="''"/>
    <xsl:param name="title-limit" select="'30'"/>
    <!--
      <fo:block>
        <xsl:value-of select="$pageclass"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="$sequence"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="$position"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="$gentext-key"/>
      </fo:block>
    body, blank, left, chapter
    -->
    <!-- sequence can be odd, even, first, blank -->
    <!-- position can be left, center, right -->
    <xsl:choose>
      <!--xsl:when test="($sequence='blank' and $position='left' and $gentext-key='chapter')">
      <xsl:variable name="text">
        <xsl:call-template name="component.title.nomarkup"/>
      </xsl:variable>
        <fo:inline keep-together.within-line="always" font-weight="bold">
          <xsl:choose>
          <xsl:when test="string-length($text) &gt; '33'">
          <xsl:value-of select="concat(substring($text, 0, $title-limit), '...')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$text"/>
        </xsl:otherwise>
        </xsl:choose>
      </fo:inline>
      </xsl:when-->
      <xsl:when test="$confidential = 1 and (($sequence='odd' and $position='left') or ($sequence='even' and $position='right'))">
        <fo:inline keep-together.within-line="always" font-weight="bold">
          <xsl:text>RED HAT CONFIDENTIAL</xsl:text>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$sequence = 'blank'">
        <!-- nothing -->
      </xsl:when>
      <!-- Extracting 'Chapter' + Chapter Number from the full Chapter title, with a dirty, dirty hack -->
      <xsl:when test="($sequence='first' and $position='left' and $gentext-key='chapter')">
        <xsl:variable name="text">
          <xsl:call-template name="component.title.nomarkup"/>
        </xsl:variable>
        <xsl:variable name="chapt">
          <xsl:value-of select="substring-before($text, '&#xA0;')"/>
        </xsl:variable>
        <xsl:variable name="remainder">
          <xsl:value-of select="substring-after($text, '&#xA0;')"/>
        </xsl:variable>
        <xsl:variable name="chapt-num">
          <xsl:value-of select="substring-before($remainder, '&#xA0;')"/>
        </xsl:variable>
        <xsl:variable name="text1">
          <xsl:value-of select="concat($chapt, '&#xA0;', $chapt-num)"/>
        </xsl:variable>
        <fo:inline keep-together.within-line="always" font-weight="bold">
          <xsl:value-of select="$text1"/>
        </fo:inline>
      </xsl:when>
      <!--xsl:when test="($sequence='odd' or $sequence='even') and $position='center'"-->
      <xsl:when test="($sequence='even' and $position='left')">
        <!--xsl:if test="$pageclass != 'titlepage'"-->
        <xsl:variable name="text">
          <xsl:call-template name="component.title.nomarkup"/>
        </xsl:variable>
        <fo:inline keep-together.within-line="always" font-weight="bold">
          <xsl:choose>
            <xsl:when test="string-length($text) &gt; '33'">
              <xsl:value-of select="concat(substring($text, 0, $title-limit), '...')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$text"/>
            </xsl:otherwise>
          </xsl:choose>
        </fo:inline>
        <!--xsl:if-->
      </xsl:when>
      <xsl:when test="($sequence='odd' and $position='right')">
        <!--xsl:if test="$pageclass != 'titlepage'"-->
        <fo:inline keep-together.within-line="always">
          <fo:retrieve-marker retrieve-class-name="section.head.marker" retrieve-position="first-including-carryover" retrieve-boundary="page-sequence"/>
        </fo:inline>
        <!--/xsl:if-->
      </xsl:when>
      <xsl:when test="$position='left'">
        <!-- Same for odd, even, empty, and blank sequences -->
        <xsl:call-template name="draft.text"/>
      </xsl:when>
      <xsl:when test="$position='center'">
        <!-- nothing for empty and blank sequences -->
      </xsl:when>
      <xsl:when test="$position='right'">
        <!-- Same for odd, even, empty, and blank sequences -->
        <xsl:call-template name="draft.text"/>
      </xsl:when>
      <xsl:when test="$sequence = 'first'">
        <!-- nothing for first pages -->
      </xsl:when>
      <xsl:when test="$sequence = 'blank'">
        <!-- nothing for blank pages -->
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!--
  From: fo/pagesetup.xsl
  Reason: Override colour
  Version: 1.72
  -->
  <xsl:template name="head.sep.rule">
    <xsl:param name="pageclass"/>
    <xsl:param name="sequence"/>
    <xsl:param name="gentext-key"/>

    <xsl:if test="$header.rule != 0">
      <xsl:attribute name="border-bottom-width">0.5pt</xsl:attribute>
      <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
      <xsl:attribute name="border-bottom-color">#000000</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!--
  From: fo/pagesetup.xsl
  Reason: Override colour
  Version: 1.72
  -->
  <xsl:template name="foot.sep.rule">
    <xsl:param name="pageclass"/>
    <xsl:param name="sequence"/>
    <xsl:param name="gentext-key"/>

    <xsl:if test="$footer.rule != 0">
      <xsl:attribute name="border-top-width">0.5pt</xsl:attribute>
      <xsl:attribute name="border-top-style">solid</xsl:attribute>
      <xsl:attribute name="border-top-color">#000000</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:param name="footnote.font.size">
    <xsl:value-of select="$body.font.master * 0.8"/><xsl:text>pt</xsl:text>
  </xsl:param>
  <xsl:param name="footnote.number.format" select="'1'"/>
  <xsl:param name="footnote.number.symbols" select="''"/>
  <xsl:attribute-set name="footnote.mark.properties">
    <xsl:attribute name="font-size">75%</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="footnote.properties">
    <xsl:attribute name="padding-top">48pt</xsl:attribute>
    <xsl:attribute name="font-family">
      <xsl:value-of select="$body.fontset"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$footnote.font.size"/>
    </xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
    <xsl:attribute name="text-align">
      <xsl:value-of select="$alignment"/>
    </xsl:attribute>
    <xsl:attribute name="start-indent">0pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="footnote.sep.leader.properties">
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="leader-pattern">rule</xsl:attribute>
    <xsl:attribute name="leader-length">1in</xsl:attribute>
  </xsl:attribute-set>

  <xsl:template match="d:author" mode="tablerow.titlepage.mode">
    <fo:table-row>
      <fo:table-cell>
        <fo:block>
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'Author'"/>
          </xsl:call-template>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:call-template name="person.name">
            <xsl:with-param name="node" select="."/>
          </xsl:call-template>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:apply-templates select="d:email"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="d:author" mode="titlepage.mode">
    <fo:block>
      <xsl:call-template name="person.name">
        <xsl:with-param name="node" select="."/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <xsl:param name="editedby.enabled">0</xsl:param>

  <xsl:template match="d:editor" mode="tablerow.titlepage.mode">
    <fo:table-row>
      <fo:table-cell>
        <fo:block>
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'Editor'"/>
          </xsl:call-template>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:call-template name="person.name">
            <xsl:with-param name="node" select="."/>
          </xsl:call-template>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:apply-templates select="d:email"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="d:othercredit" mode="tablerow.titlepage.mode">
    <fo:table-row>
      <fo:table-cell>
        <fo:block>
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'translator'"/>
          </xsl:call-template>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:call-template name="person.name">
            <xsl:with-param name="node" select="."/>
          </xsl:call-template>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:apply-templates select="d:email"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="d:title" mode="book.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.recto.style" text-align="center" font-size="34pt" space-before="18.6624pt" font-weight="bold" font-family="{$title.fontset}">
      <xsl:call-template name="division.title">
        <xsl:with-param name="node" select="ancestor-or-self::d:book[1]"/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <xsl:template match="d:subtitle" mode="book.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.recto.style" text-align="center" font-size="20pt" space-before="30pt" font-family="{$title.fontset}">
      <xsl:apply-templates select="." mode="book.titlepage.recto.mode"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="d:issuenum" mode="book.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.recto.style" text-align="center" font-size="16pt" space-before="15.552pt" font-family="{$title.fontset}">
      <xsl:apply-templates select="." mode="book.titlepage.recto.mode"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="d:author" mode="book.titlepage.recto.auto.mode">
    <fo:block xsl:use-attribute-sets="book.titlepage.recto.style" font-size="14pt" space-before="15.552pt">
      <xsl:call-template name="person.name">
        <xsl:with-param name="node" select="."/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <!-- Use our own slightly simpler title page (just show title, version, authors) -->
  <xsl:template name="book.titlepage.recto">
  
    <xsl:choose>
      <xsl:when test="d:bookinfo/d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                             select="d:bookinfo/d:title"/>
      </xsl:when>
      <xsl:when test="d:info/d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                             select="d:info/d:title"/>
      </xsl:when>
      <xsl:when test="d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                             select="d:title"/>
      </xsl:when>
    </xsl:choose>

    <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                         select="d:bookinfo/d:issuenum"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                         select="d:info/d:issuenum"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                         select="d:issuenum"/>

    <xsl:choose>
      <xsl:when test="d:bookinfo/d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                             select="d:bookinfo/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:info/d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                             select="d:info/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                             select="d:subtitle"/>
      </xsl:when>
    </xsl:choose>

    <fo:block xsl:use-attribute-sets="book.titlepage.recto.style"
              font-size="14pt" space-before="15.552pt">
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                           select="d:bookinfo/d:releaseinfo"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode"
                           select="d:info/d:releaseinfo"/>
    </fo:block>

    <fo:block text-align="center" space-before="15.552pt">
      <xsl:call-template name="person.name.list">
        <xsl:with-param name="person.list" select="d:bookinfo/d:authorgroup/d:author|d:bookinfo/d:authorgroup/d:corpauthor|d:info/d:authorgroup/d:author"/>
        <xsl:with-param name="person.type" select="'author'"/>
      </xsl:call-template>
    </fo:block>

    <fo:block text-align="center" space-before="15.552pt">
      <xsl:call-template name="person.name.list">
        <xsl:with-param name="person.list" select="d:bookinfo/d:authorgroup/d:editor|d:info/d:authorgroup/d:editor"/>
        <xsl:with-param name="person.type" select="'editor'"/>
      </xsl:call-template>
    </fo:block>

    <fo:block text-align="center" space-before="15.552pt">
      <xsl:call-template name="person.name.list">
        <xsl:with-param name="person.list" select="d:bookinfo/d:authorgroup/d:othercredit|d:info/d:authorgroup/d:othercredit"/>
        <xsl:with-param name="person.type" select="'othercredit'"/>
      </xsl:call-template>
    </fo:block>
    
            <fo:block text-align="center" space-before="15.552pt">
                <fo:table table-layout="fixed" width="175mm">
                    <fo:table-column column-width="175mm"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell text-align="center">
                                <fo:block text-align="center">
                                	Copyright 2006 - 2018 by EsperTech Inc.
                                </fo:block>
				<fo:block>
				    <fo:external-graphic src="url(../../staging/images/images/esper_logo.png)"/>
				</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>
            </fo:block>
            

  </xsl:template>

  <!-- Make examples, tables etc. break across pages -->
  <xsl:attribute-set name="formal.object.properties">
    <xsl:attribute name="keep-together.within-column">auto</xsl:attribute>
  </xsl:attribute-set>

  <!-- Correct placement of titles for figures and examples. -->
  <xsl:param name="formal.title.placement">
    figure after example before equation before table before procedure before
  </xsl:param>

  <!-- Prevent blank pages in output -->
  <xsl:template name="book.titlepage.before.verso"></xsl:template>
  <xsl:template name="book.titlepage.separator"></xsl:template>
  <xsl:template name="book.titlepage.before.recto"></xsl:template>
  <xsl:template name="book.titlepage.verso"></xsl:template>
  <xsl:template name="book.titlepage3.recto"></xsl:template>

  <xsl:template name="book.titlepage">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format">
      <xsl:call-template name="book.titlepage.before.recto"/>
      <fo:block>
        <xsl:call-template name="book.titlepage.recto"/>
      </fo:block>
      <xsl:call-template name="book.titlepage.separator"/>
      <fo:block>
        <xsl:call-template name="book.titlepage.verso"/>
      </fo:block>
      <xsl:call-template name="book.titlepage.separator"/>
      <fo:block>
        <xsl:call-template name="book.titlepage3.recto"/>
      </fo:block>
      <xsl:call-template name="book.titlepage.separator"/>
    </fo:block>
  </xsl:template>

  <!--
  From: fo/qandaset.xsl
  Reason: Id in list-item-label causes fop crash
  Version:1.72
  -->

  <xsl:template match="d:question">
    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>

    <xsl:variable name="entry.id">
      <xsl:call-template name="object.id">
        <xsl:with-param name="object" select="parent::*"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="deflabel">
      <xsl:choose>
        <xsl:when test="ancestor-or-self::*[@defaultlabel]">
          <xsl:value-of select="(ancestor-or-self::*[@defaultlabel])[last()]
                              /@defaultlabel"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$qanda.defaultlabel"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <fo:list-item id="{$entry.id}" xsl:use-attribute-sets="list.item.spacing">
      <fo:list-item-label end-indent="label-end()">
        <xsl:choose>
          <xsl:when test="$deflabel = 'none'">
            <fo:block/>
          </xsl:when>
          <xsl:otherwise>
            <fo:block>
              <xsl:apply-templates select="." mode="label.markup"/>
              <xsl:if test="$deflabel = 'number' and not(label)">
                <xsl:apply-templates select="." mode="intralabel.punctuation"/>
              </xsl:if>
            </fo:block>
          </xsl:otherwise>
        </xsl:choose>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <xsl:choose>
          <xsl:when test="$deflabel = 'none'">
            <fo:block font-weight="bold">
              <xsl:apply-templates select="*[local-name(.)!='label']"/>
            </fo:block>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="*[local-name(.)!='label']"/>
          </xsl:otherwise>
        </xsl:choose>
        <!-- Uncomment this line to get revhistory output in the question -->
        <!-- <xsl:apply-templates select="preceding-sibling::d:revhistory"/> -->
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <!--
  From: fo/qandaset.xsl
  Reason: Id in list-item-label causes fop crash
  Version:1.72
  -->
  <xsl:template match="d:answer">
    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>
    <xsl:variable name="entry.id">
      <xsl:call-template name="object.id">
        <xsl:with-param name="object" select="parent::*"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="deflabel">
      <xsl:choose>
        <xsl:when test="ancestor-or-self::*[@defaultlabel]">
          <xsl:value-of select="(ancestor-or-self::*[@defaultlabel])[last()]
                              /@defaultlabel"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$qanda.defaultlabel"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <fo:list-item xsl:use-attribute-sets="list.item.spacing">
      <fo:list-item-label end-indent="label-end()">
        <xsl:choose>
          <xsl:when test="$deflabel = 'none'">
            <fo:block/>
          </xsl:when>
          <xsl:otherwise>
            <fo:block>
              <xsl:variable name="answer.label">
                <xsl:apply-templates select="." mode="label.markup"/>
              </xsl:variable>
              <xsl:copy-of select="$answer.label"/>
            </fo:block>
          </xsl:otherwise>
        </xsl:choose>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <xsl:apply-templates select="*[local-name(.)!='label']"/>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>


  <xsl:template match="d:programlisting">

    <xsl:variable name="language">
      <xsl:value-of select="s:toUpperCase(string(@language))" xmlns:s="java:java.lang.String"/>
    </xsl:variable>

    <xsl:variable name="hilighter" select="jbh:new()"/>
    <xsl:variable name="parsable" select="jbh:isParsable($language)"/>

    <fo:block background-color="#F5F5F5"
              border-style="solid"
              border-width=".3mm"
              border-color="#CCCCCC"
              font-family="{$programlisting.font}"
              font-size="{$programlisting.font.size}"
              space-before="12pt"
              space-after="12pt"
              linefeed-treatment="preserve"
              white-space-collapse="false"
              white-space-treatment="preserve"
              padding-bottom="12pt"
              padding-top="12pt"
              padding-right="12pt"
              padding-left="12pt">

      <xsl:choose>
        <xsl:when test="$parsable = 'true'">
          <xsl:for-each select="node()">
            <xsl:choose>
              <xsl:when test="self::text()">
                <xsl:variable name="child.content" select="."/>

                <xsl:variable name="caller" select="jbh:parseText($hilighter, $language, string($child.content), 'UTF-8')"/>
                <xsl:variable name="noOfTokens" select="jbh:getNoOfTokens($caller)"/>

                <xsl:call-template name="iterator">
                  <xsl:with-param name="caller" select="$caller"/>
                  <xsl:with-param name="noOfTokens" select="$noOfTokens"/>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>
                <fo:inline>
                  <xsl:call-template name="anchor"/>
                  <xsl:apply-templates select="." mode="callout-bug"/>
                </fo:inline>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>

    </fo:block>
  </xsl:template>


  <xsl:template name="iterator">
    <xsl:param name="caller"/>
    <xsl:param name="noOfTokens"/>
    <xsl:param name="i" select="0"/>

    <xsl:variable name="style" select="jbh:getStyle($caller, $i)"/>
    <xsl:variable name="token" select="jbh:getToken($caller, $i)"/>

    <xsl:choose>
      <xsl:when test="$style = 'java_keyword'">
        <fo:inline color="#7F1B55" font-weight="bold">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_plain'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_type'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_separator'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_literal'">
        <fo:inline color="#2A00FF">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_comment'">
        <fo:inline color="#3F7F5F">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_javadoc_comment'">
        <fo:inline color="#3F5FBF" font-style="italic">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_operator'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'java_javadoc_tag'">
        <fo:inline color="#7F9FBF" font-weight="bold" font-style="italic">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_plain'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_char_data'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_tag_symbols'">
        <fo:inline color="#008080">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_comment'">
        <fo:inline color="#3F5FBF">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_attribute_value'">
        <fo:inline color="#2A00FF">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_attribute_name'">
        <fo:inline color="#7F007F" font-weight="bold">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_processing_instruction'">
        <fo:inline color="#000000" font-weight="bold" font-style="italic">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_tag_name'">
        <fo:inline color="#3F7F7F">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_rife_tag'">
        <fo:inline color="#000000">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$style = 'xml_rife_name'">
        <fo:inline color="#008CCA">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline color="black">
          <xsl:value-of select="$token"/>
        </fo:inline>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="$i &lt; $noOfTokens - 1">
      <xsl:call-template name="iterator">
        <xsl:with-param name="caller" select="$caller"/>
        <xsl:with-param name="noOfTokens" select="$noOfTokens"/>
        <xsl:with-param name="i" select="$i + 1"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!--
  From: fo/graphics.xsl
  Reason: Scale PDF images down to fit the page (content-width="scale-down-to-fit" content-height="scale-down-to-fit").
  Version: 1.74.0
  -->
  <xsl:template name="process.image">
    <!-- When this template is called, the current node should be  -->
    <!-- a graphic, inlinegraphic, imagedata, or videodata. All    -->
    <!-- those elements have the same set of attributes, so we can -->
    <!-- handle them all in one place.                             -->

    <xsl:variable name="scalefit">
      <xsl:choose>
        <xsl:when test="$ignore.image.scaling != 0">0</xsl:when>
        <xsl:when test="@contentwidth">0</xsl:when>
        <xsl:when test="@contentdepth and
                      @contentdepth != '100%'">0
        </xsl:when>
        <xsl:when test="@scale">0</xsl:when>
        <xsl:when test="@scalefit">
          <xsl:value-of select="@scalefit"/>
        </xsl:when>
        <xsl:when test="@width or @depth">1</xsl:when>
        <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="scale">
      <xsl:choose>
        <xsl:when test="$ignore.image.scaling != 0">0</xsl:when>
        <xsl:when test="@contentwidth or @contentdepth">1.0</xsl:when>
        <xsl:when test="@scale">
          <xsl:value-of select="@scale div 100.0"/>
        </xsl:when>
        <xsl:otherwise>1.0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="filename">
      <xsl:choose>
        <xsl:when test="local-name(.) = 'graphic'
                      or local-name(.) = 'inlinegraphic'">
          <!-- handle legacy graphic and inlinegraphic by new template -->
          <xsl:call-template name="mediaobject.filename">
            <xsl:with-param name="object" select="."/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <!-- imagedata, videodata, audiodata -->
          <xsl:call-template name="mediaobject.filename">
            <xsl:with-param name="object" select=".."/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="content-type">
      <xsl:if test="@format">
        <xsl:call-template name="graphic.format.content-type">
          <xsl:with-param name="format" select="@format"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="bgcolor">
      <xsl:call-template name="pi.dbfo_background-color">
        <xsl:with-param name="node" select=".."/>
      </xsl:call-template>
    </xsl:variable>

    <fo:external-graphic>
      <xsl:attribute name="src">
        <xsl:call-template name="fo-external-image">
          <xsl:with-param name="filename">
            <xsl:if test="$img.src.path != '' and
                        not(starts-with($filename, '/')) and
                        not(contains($filename, '://'))">
              <xsl:value-of select="$img.src.path"/>
            </xsl:if>
            <xsl:value-of select="$filename"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:attribute>

      <xsl:attribute name="width">
        <xsl:choose>
          <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
          <xsl:when test="contains(@width,'%')">
            <xsl:value-of select="@width"/>
          </xsl:when>
          <xsl:when test="@width and not(@width = '')">
            <xsl:call-template name="length-spec">
              <xsl:with-param name="length" select="@width"/>
              <xsl:with-param name="default.units" select="'px'"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="not(@depth) and $default.image.width != ''">
            <xsl:call-template name="length-spec">
              <xsl:with-param name="length" select="$default.image.width"/>
              <xsl:with-param name="default.units" select="'px'"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>auto</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>

      <xsl:attribute name="height">
        <xsl:choose>
          <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
          <xsl:when test="contains(@depth,'%')">
            <xsl:value-of select="@depth"/>
          </xsl:when>
          <xsl:when test="@depth">
            <xsl:call-template name="length-spec">
              <xsl:with-param name="length" select="@depth"/>
              <xsl:with-param name="default.units" select="'px'"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>auto</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>

      <xsl:attribute name="content-width">
        <xsl:choose>
          <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
          <xsl:when test="contains(@contentwidth,'%')">
            <xsl:value-of select="@contentwidth"/>
          </xsl:when>
          <xsl:when test="@contentwidth">
            <xsl:call-template name="length-spec">
              <xsl:with-param name="length" select="@contentwidth"/>
              <xsl:with-param name="default.units" select="'px'"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="number($scale) != 1.0">
            <xsl:value-of select="$scale * 100"/>
            <xsl:text>%</xsl:text>
          </xsl:when>
          <xsl:when test="$scalefit = 1">scale-to-fit</xsl:when>
          <xsl:otherwise>scale-down-to-fit</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>

      <xsl:attribute name="content-height">
        <xsl:choose>
          <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
          <xsl:when test="contains(@contentdepth,'%')">
            <xsl:value-of select="@contentdepth"/>
          </xsl:when>
          <xsl:when test="@contentdepth">
            <xsl:call-template name="length-spec">
              <xsl:with-param name="length" select="@contentdepth"/>
              <xsl:with-param name="default.units" select="'px'"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="number($scale) != 1.0">
            <xsl:value-of select="$scale * 100"/>
            <xsl:text>%</xsl:text>
          </xsl:when>
          <xsl:when test="$scalefit = 1">scale-to-fit</xsl:when>
          <xsl:otherwise>scale-down-to-fit</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>

      <xsl:if test="$content-type != ''">
        <xsl:attribute name="content-type">
          <xsl:value-of select="concat('content-type:',$content-type)"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="$bgcolor != ''">
        <xsl:attribute name="background-color">
          <xsl:value-of select="$bgcolor"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="@align">
        <xsl:attribute name="text-align">
          <xsl:value-of select="@align"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="@valign">
        <xsl:attribute name="display-align">
          <xsl:choose>
            <xsl:when test="@valign = 'top'">before</xsl:when>
            <xsl:when test="@valign = 'middle'">center</xsl:when>
            <xsl:when test="@valign = 'bottom'">after</xsl:when>
            <xsl:otherwise>auto</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
    </fo:external-graphic>
  </xsl:template>

</xsl:stylesheet>
