package com.gxj.demo.region;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "LegacyRegion")
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String level;

    @Column(name = "parent_name")
    private String parentName;

    @Column(length = 1000)
    private String description;

    protected Region() {
    }

    public Region(String name, String level, String parentName, String description) {
        this.name = name;
        this.level = level;
        this.parentName = parentName;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public String getParentName() {
        return parentName;
    }

    public String getDescription() {
        return description;
    }
}
