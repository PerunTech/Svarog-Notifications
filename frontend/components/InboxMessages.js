import { React, connect, PropTypes, axios, createHashHistory, elements } from 'perun-core'
const { alertUser } = elements
import style from './style/MessagesHolder.module.css'
import { iconManager } from './svgHolder';
import format from 'date-fns/format'
import en from 'date-fns/locale/en-US'
import './style/inbox-message.css'
import { jsonToURI } from '../utils/utils';

const tableName = 'MESSAGE'

class InboxMessages extends React.Component {
  constructor(props) {
    super(props);
    this.state = {

    };
  }

  componentDidMount() {
    this.getInboxSubjectRecipientInfo()
  }

  getMessageSubject = () => {
    const { svSession, objId } = this.props
    const url = window.server + `/svarog_notifications/services/getObjectsByParentId/${svSession}/${objId}/${tableName}/PKID/DESC`
    axios.get(url)
      .then((response) => {
        if (response.data) {
          this.generateHtmlInboxSubject(response.data)

        }
      })
      .catch((error) => {
        console.error('error')
      })
  }


  getInboxSubjectRecipientInfo = () => {
    const { svSession } = this.props
    const url = window.server + `/svarog_notifications/services/getInboxSubjectRecipientInfo/${svSession}`
    axios.get(url)
      .then((response) => {
        if (response.data) {
          this.iterateRecipientInfo(response.data)
          this.getMessageSubject()
        }
      })
      .catch((error) => {
        console.error('error')
      })
  }

  iterateRecipientInfo = (recipientData) => {
    const { objId } = this.props
    const recipientInfo = recipientData.filter(recipient => recipient.SUBJECT.object_id === objId)
    this.setState({ recipientState: recipientInfo })
    let recipients = []
    let recipientsObjIds = []
    let ccRecipients = []
    let ccRecipientsObjIds = []
    let bccRecipients = []
    let bccRecipientsObjIds = []
    for (let i = 0; i < recipientInfo.length; i++) {
      let subjectObjId = recipientInfo[i].SUBJECT.object_id
      let category = recipientInfo[i].SUBJECT.CATEGORY
      let priority = recipientInfo[i].SUBJECT.PRIORITY
      let moduleName = recipientInfo[i].SUBJECT.MODULE_NAME
      let title = recipientInfo[i].SUBJECT.TITLE
      this.setState({ subjectObjIdState: subjectObjId, category: category, priority: priority, moduleName: moduleName, title: title })
      if (recipientInfo[i] && recipientInfo[i].MSG_TO && recipientInfo[i].MSG_TO.items && Array.isArray(recipientInfo[i].MSG_TO.items) && recipientInfo[i].MSG_TO.items.length > 0) {
        for (let j = 0; j < recipientInfo[i].MSG_TO.items.length; j++) {
          recipients.push(recipientInfo[i].MSG_TO.items[j].USER_NAME)
          recipientsObjIds.push(recipientInfo[i].MSG_TO.items[j].object_id)
        }
        this.setState({ recipients, recipientsObjIds })
      }

      if (recipientInfo[i] && recipientInfo[i].MSG_CC && recipientInfo[i].MSG_CC.items && Array.isArray(recipientInfo[i].MSG_CC.items) && recipientInfo[i].MSG_CC.items.length > 0) {
        for (let k = 0; k < recipientInfo[i].MSG_CC.items.length; k++) {
          ccRecipients.push(recipientInfo[i].MSG_CC.items[k].USER_NAME)
          ccRecipientsObjIds.push(recipientInfo[i].MSG_CC.items[i].object_id)
        }
        this.setState({ ccRecipients, ccRecipientsObjIds })
      }

      if (recipientInfo[i] && recipientInfo[i].MSG_BCC && recipientInfo[i].MSG_BCC.items && Array.isArray(recipientInfo[i].MSG_BCC.items) && recipientInfo[i].MSG_BCC.items.length > 0) {
        for (let m = 0; m < recipientInfo[i].MSG_CC.items.length; m++) {
          bccRecipients.push(recipientInfo[i].MSG_BCC.items[m].USER_NAME)
          bccRecipientsObjIds.push(recipientInfo[i].MSG_BCC.items[i].object_id)
        }
        this.setState({ bccRecipients, bccRecipientsObjIds })
      }
    }
  }


  generateHtmlInboxSubject = (subjectData) => {
    const { objId } = this.props
    let htmlSubjectElement
    let elementArr = []
    let className
    let labelCode
    let recipients = this.state.recipients
    let ccRecipients = this.state.ccRecipients
    let bccRecipients = this.state.bccRecipients
    if (subjectData) {
      for (let i = 0; i < subjectData.length; i++) {
        if (subjectData[i]["MESSAGE.PRIORITY"] === '1') {
          className = 'low-priority'
          labelCode = 'Low'
        }
        if (subjectData[i]["MESSAGE.PRIORITY"] === '2') {
          className = 'normal-priority'
          labelCode = 'Normal'
        }
        if (subjectData[i]["MESSAGE.PRIORITY"] === '3') {
          className = 'high-priority'
          labelCode = 'High'
        }
        const createByName = subjectData[i]["MESSAGE.CREATED_BY_USERNAME"]
        const messageText = subjectData[i]["MESSAGE.TEXT"]
        const date = subjectData[i]["MESSAGE.DT_INSERT"]
        htmlSubjectElement = <div className={'message-holder'}>
          <div className='msg-title-holder'>
            <p type='text' className={'msg-title'}>{iconManager.getIcon('messageIcon')}<strong style={{ marginLeft: '0.5%' }}>{this.state.title}</strong></p>
          </div>
          <div className={'message-data'}>
            <p type='text' className={'create-holder'}><b>{iconManager.getIcon('user')}{createByName}</b></p>
            <p type='text' className={'date-holder'}>
              {format(new Date(date), 'eee MMM dd kk:mm', { locale: en })}
            </p>
            <p type='text' className={`priority-paragraph ${className}`}>Priority: {labelCode}</p>
            <p type='text' className={'assigned-to'}>To: <strong className={'recipinets'}>{recipients?.join(', ')}</strong></p>
            <p type='text' className={'assigned-to'}>Cc: <strong className={'recipinets'}>{ccRecipients?.join(', ')}</strong></p>
            <p type='text' className={'assigned-to'}>Bcc: <strong className={'recipinets'}>{bccRecipients?.join(', ')}</strong></p>
          </div>
          <div className={'message-subject-holder'}>
            <p className={'message-paragraph'}>Message: </p>
            <p type='text'>{messageText}</p>
          </div>
        </div>

        elementArr.push(htmlSubjectElement)
      }
      this.setState({ generatedValues: elementArr })
    }
  }

  replyFunc = (e) => {
    const { messageText } = this.state
    let htmlrReplyText
    let replayElementArr = []
    htmlrReplyText = <div className='reply-msg-holder'>
      <textarea name="messageText" id="messageText" onChange={this.onChange} value={messageText} className="reply-textarea" placeholder="Type your message here"></textarea>
      <button onClick={() => this.closeMessage()} className='cancel-btn'>Cancel</button>
    </div>

    replayElementArr.push(htmlrReplyText)

    this.setState({ generatedReplyValues: replayElementArr })
  }

  onChange = e => {
    this.setState({ [e.target.name]: e.target.value })
  }


  sentReply = (e) => {
    const { subjectObjIdState, messageText, recipientsObjIds, ccRecipientsObjIds, bccRecipientsObjIds, category, priority, moduleName, title } = this.state
    const replyData = { SUBJECT_OBJ_ID: subjectObjIdState, TEXT: messageText, CUSTOM_RECIPIENTS: '', CUSTOM_CC: '', CUSTOM_BCC: '', MSG_ATTACHMENT: '', PRIORITY: priority, CATEGORY: category, MODULE_NAME: moduleName, TITLE: title }
    if (Array.isArray(recipientsObjIds) && recipientsObjIds.length > 0) {
      Object.assign(replyData, { CUSTOM_RECIPIENTS: recipientsObjIds.join(',') })
    }
    if (Array.isArray(ccRecipientsObjIds) && ccRecipientsObjIds.length > 0) {
      Object.assign(replyData, { CUSTOM_CC: ccRecipientsObjIds.join(',') })
    }
    if (Array.isArray(bccRecipientsObjIds) && bccRecipientsObjIds.length > 0) {
      Object.assign(replyData, { CUSTOM_BCC: bccRecipientsObjIds.join(',') })
    }
    const data = jsonToURI(replyData)
    let { svSession } = this.props
    let postUrl = window.server + '/svarog_notifications/services/createNewMessage/' + svSession
    axios({
      method: 'post',
      data: data,
      url: postUrl,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    }).then((response) => {
      if (response && response.data) {
        if (response.data) {
          alertUser(true, response.data.type.toLowerCase(), response.data.title, response.data.message, null)
          if (response.data.type, 'success') {
            this.setState({ ['messageText']: '', generatedReplyValues: '' }, () => this.getMessageSubject())
          }
        }
      }
    })
      .catch((error) => {
        if (error.data) {
          alertUser(true, error.data.type.toLowerCase(), response.data.title, error.data.message)
        }
      })
  }


  closeMessage = () => {
    this.setState({ generatedReplyValues: false })
  }

  render() {
    const { generatedValues, titleMsg, generatedReplyValues } = this.state
    return (
      <React.Fragment>
        {titleMsg}
        <div className={style['context-menu-holder']}>
          <p className={style['inbox-paragraph']}>Inbox Message</p>
          <button className='btnBack' onClick={() => this.props.handleBack()}>{iconManager.getIcon('back')}Back</button>
          {generatedValues}
          {generatedReplyValues}
          <button onClick={(e) => generatedReplyValues ? this.sentReply(e) : this.replyFunc(e)} className='reply-msg'>Reply</button>
        </div>
      </React.Fragment>
    )
  }
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

InboxMessages.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(InboxMessages)