package com.internship.importer.mapper;

import com.internship.importer.model.Company;
import com.internship.importer.model.CompanyIndustry;
import com.internship.importer.model.Industry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyMapper {
    void insertCompanies(@Param("list")List<Company> companies);

    void insertIndustries(@Param("list")List<Industry> industries);

    void insertCompanyIndustries(@Param("list")List<CompanyIndustry> companies);
}
