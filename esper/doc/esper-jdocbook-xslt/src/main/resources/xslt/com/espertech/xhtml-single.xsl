<?xml version="1.0" encoding="utf-8"?>
<!--
    Copyright 2008 JBoss, a division of Red Hat
    License: GPL
    Author: Jeff Fearn <jfearn@redhat.com>
    Author: Tammy Fox <tfox@redhat.com>
    Author: Andy Fitzsimon <afitzsim@redhat.com>
    Author: Mark Newton <mark.newton@jboss.org>
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:d="http://docbook.org/ns/docbook" version="1.0">

  <xsl:import href="http://docbook.sourceforge.net/release/xsl/1.72.0/xhtml/docbook.xsl"/>

  <xsl:include href="xhtml-common.xsl"/>

  <!--
  From: xhtml/titlepage-templates.xsl
  Reason: Needed to add JBoss.org and Community Documentation graphics to header
  Version: 1.72.0
  -->
  <xsl:template name="book.titlepage.recto">
    <p xmlns="http://www.w3.org/1999/xhtml">
      <xsl:attribute name="id">
        <xsl:text>title</xsl:text>
      </xsl:attribute>
      <a>
        <xsl:attribute name="href">
          <xsl:value-of select="$siteHref"/>
        </xsl:attribute>
        <xsl:attribute name="class">
          <xsl:text>site_href</xsl:text>
        </xsl:attribute>
        <strong>
          <xsl:value-of select="$siteLinkText"/>
        </strong>
      </a>
      <a>
        <xsl:attribute name="href">
          <xsl:value-of select="$docHref"/>
        </xsl:attribute>
        <xsl:attribute name="class">
          <xsl:text>doc_href</xsl:text>
        </xsl:attribute>
        <strong>
          <xsl:value-of select="$docLinkText"/>
        </strong>
      </a>
    </p>
    <xsl:choose>
      <xsl:when test="d:bookinfo/d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:title"/>
      </xsl:when>
      <xsl:when test="d:info/d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:title"/>
      </xsl:when>
      <xsl:when test="d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:title"/>
      </xsl:when>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="d:bookinfo/d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:info/d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:subtitle"/>
      </xsl:when>
    </xsl:choose>

    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:corpauthor"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:corpauthor"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:authorgroup"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:authorgroup"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:author"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:author"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:othercredit"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:othercredit"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:releaseinfo"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:releaseinfo"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:copyright"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:copyright"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:legalnotice"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:legalnotice"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:pubdate"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:pubdate"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:revision"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:revision"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:revhistory"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:revhistory"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:abstract"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:abstract"/>
  </xsl:template>

</xsl:stylesheet>
