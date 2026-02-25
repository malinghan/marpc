package com.malinghan.marpc.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int id;
    private String item;
    private double price;
    private int userId;
}
