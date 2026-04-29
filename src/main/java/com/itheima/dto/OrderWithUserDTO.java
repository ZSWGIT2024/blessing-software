// OrderWithUserDTO.java
package com.itheima.dto;

import com.itheima.pojo.VipOrder;
import lombok.Data;
import java.util.List;

@Data
public class OrderWithUserDTO {
    private List<VipOrder> list;
    private Integer page;
    private Integer pageSize;
    private Integer total;
}