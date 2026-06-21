export function updateFormField(form, key, value) {
  form.value = { ...form.value, [key]: value };
}

export function emptyAddressForm() {
  return {
    id: null,
    realName: "",
    phone: "",
    province: "",
    city: "",
    district: "",
    detail: "",
    isDefault: false
  };
}

export function emptyRefundForm(orderId = "") {
  return {
    uni: orderId,
    text: "",
    explain: ""
  };
}

export function emptyCommentForm() {
  return {
    productScore: 5,
    serviceScore: 5,
    comment: ""
  };
}

export function emptyExtractCashForm(bankName = "") {
  return {
    name: "",
    cardum: "",
    bankName,
    wechat: "",
    qrcodeUrl: "",
    money: ""
  };
}
