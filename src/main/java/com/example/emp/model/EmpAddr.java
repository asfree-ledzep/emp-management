package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpAddr {
    private Integer empno;
    private String  zipcode;
    private String  address;
    private String  addrDetail;
}
