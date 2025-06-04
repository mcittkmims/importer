package com.internship.importer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.cursor.Cursor;

import java.util.stream.Stream;

@Mapper
public interface CompanyMapper {
    Cursor<String> getJsonData();
}
