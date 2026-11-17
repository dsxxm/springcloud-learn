package com.baka.es;

/**
 * Product
 * 用于测试ES建立文档的product类
 */
public class Product {

	private Long id;
	private String name;
	private String price;

	public Product() {
	}

	public Product(Long id, String name, String price) {
		this.id = id;
		this.name = name;
		this.price = price;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
